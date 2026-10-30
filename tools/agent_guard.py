#!/usr/bin/env python3
"""Claude Code PreToolUse guard: the repository's "never" rules, enforced instead of only written.

Agents often run with permission prompts off. A rule in AGENTS.md is then only as strong as the
agent's attention, but a PreToolUse hook's "deny" holds in every permission mode. It refuses:

  1. pushing `main` (a refspec whose destination is main, or a bare `git push` while on main),
     `--all` / `--mirror`, a force push (`--force-with-lease` on a feature branch passes), and
     `git push --no-verify`;
  2. skipping or rewiring git hooks: `git commit --no-verify` / `-n`, changing `core.hooksPath`;
  3. throwing work away: `git reset --hard`, `git clean -f`, `git checkout -- .`, `git restore .`;
  4. destroying data: DROP / TRUNCATE sent to a database client, `atlas schema clean`,
     `docker volume rm|prune`, `docker compose down -v`, `docker system prune --volumes`;
  5. loosening the zero-findings gate: writing a lint baseline file, or running `detektBaseline`;
  6. stopping shared daemons on a shared machine: `gradlew --stop`, `pkill`/`killall` of java,
     Gradle or Kotlin.

It reads the command with `shlex`, splits it at line breaks, `;`, `&&`, `||` and `|`, skips
heredoc bodies, resolves `HEAD` to the current branch, and looks inside `bash -c` / `sh -c`. Text that only mentions a rule (a commit message, `grep`) passes. It catches an
agent's mistake; it is not a sandbox against deliberate evasion.

Failing open: bad input or a bug here prints one line to stderr and exits 0 with no decision, so a
guard bug never blocks every session. A denial is the documented
`hookSpecificOutput.permissionDecision: "deny"` JSON on stdout.

Try it without a session:  python3 tools/agent_guard.py --check 'git push origin main'
Tests:                     python3 -m unittest discover -s tools -p 'test_*.py'
"""

from __future__ import annotations

import json
import re
import shlex
import subprocess
import sys

PROTECTED = {"main", "master"}
SHELLS = {"bash", "sh", "zsh"}
DB_CLIENTS = {"psql", "sqlite3", "mysql", "pgcli"}
SEPARATORS = {";", "&&", "||", "|", "&"}
SHARED_PROCESSES = re.compile(r"java|gradle|kotlin", re.IGNORECASE)
BASELINE_FILE = re.compile(r"(^|/)[^/]*baseline[^/]*\.xml$", re.IGNORECASE)
DESTRUCTIVE_SQL = re.compile(r"\b(DROP\s+(TABLE|DATABASE|SCHEMA)|TRUNCATE)\b", re.IGNORECASE)


HEREDOC = re.compile(r"<<-?\s*['\"]?(\w+)['\"]?")


def join_lines(command: str) -> str:
    """Turns unquoted line breaks into `;` and drops heredoc bodies, which are data.

    shlex reads a line break as plain whitespace, so without this a second line would become
    arguments of the first command and skip every rule.
    """
    lines, out, quote, heredoc = command.split("\n"), [], None, None
    for line in lines:
        if heredoc:
            if line.strip() == heredoc:
                heredoc = None
            continue
        out.append(line)
        if quote is None:
            match = HEREDOC.search(line)
            heredoc = match.group(1) if match else None
        i = 0
        while i < len(line):
            char = line[i]
            if char == "\\" and quote != "'":
                i += 1
            elif quote is None and char in "'\"":
                quote = char
            elif char == quote:
                quote = None
            i += 1
        out.append("\n" if quote else ";")
    return "".join(out)


def split_commands(command: str) -> list[list[str]]:
    """Splits a shell line into simple commands, each a list of words."""
    lexer = shlex.shlex(join_lines(command), posix=True, punctuation_chars=";&|")
    lexer.whitespace_split = True
    commands, current = [], []
    for token in lexer:
        if token in SEPARATORS:
            if current:
                commands.append(current)
            current = []
        else:
            current.append(token)
    if current:
        commands.append(current)
    nested = []
    for words in commands:
        if words and words[0] in SHELLS and "-c" in words[:-1]:
            nested.extend(split_commands(words[words.index("-c") + 1]))
    return commands + nested


def strip_env(words: list[str]) -> list[str]:
    """Drops leading `VAR=value` assignments and `env`."""
    i = 0
    while i < len(words) and (re.match(r"^\w+=", words[i]) or words[i] == "env"):
        i += 1
    return words[i:]


def current_branch() -> str:
    try:
        out = subprocess.run(
            ["git", "branch", "--show-current"], capture_output=True, text=True, timeout=3, check=False
        )
        return out.stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return ""


def check_git(args: list[str], branch_of=current_branch) -> str | None:
    if "-c" in args and any(a.startswith("core.hooksPath") for a in args):
        return "Changing core.hooksPath skips the repository's git hooks."
    while args and args[0] in ("-C", "-c"):
        args = args[2:]
    if not args:
        return None
    sub, rest = args[0], args[1:]
    if sub == "push":
        return check_push(rest, branch_of)
    if sub == "commit" and ("--no-verify" in rest or any(re.match(r"^-[a-zA-Z]*n[a-zA-Z]*$", a) for a in rest)):
        return "`git commit --no-verify` skips the hooks; fix what they report instead."
    if sub == "config" and any(a.startswith("core.hooksPath") for a in rest):
        return "Changing core.hooksPath skips the repository's git hooks."
    if sub == "reset" and "--hard" in rest:
        return "`git reset --hard` discards work. Commit or stash it by hand if you mean it."
    if sub == "clean" and any(re.match(r"^-[a-zA-Z]*f", a) or a == "--force" for a in rest):
        return "`git clean -f` deletes untracked files for good."
    unstage_only = sub == "restore" and "--staged" in rest and "--worktree" not in rest and "-W" not in rest
    if sub in ("checkout", "restore") and "." in rest and not unstage_only:
        return f"`git {sub} .` discards every uncommitted change."
    return None


def check_push(rest: list[str], branch_of) -> str | None:
    if "--no-verify" in rest:
        return "`git push --no-verify` skips the pre-push hook."
    if "--all" in rest or "--mirror" in rest:
        return "`git push --all/--mirror` pushes main too."
    if "--force" in rest or "-f" in rest or any(a.startswith("+") for a in rest):
        return "Force pushes rewrite shared history; use --force-with-lease on your own branch."
    refspecs = [a for a in rest if not a.startswith("-")][1:]
    if not refspecs:
        branch = branch_of()
        return f"Pushing from `{branch}` updates it directly; open a pull request." if branch in PROTECTED else None
    for spec in refspecs:
        target = spec.split(":")[-1].removeprefix("refs/heads/")
        if target in ("HEAD", "@"):
            target = branch_of()
        if target in PROTECTED:
            return f"Pushing to `{target}` bypasses review; open a pull request."
    return None


def check_words(words: list[str], full: str, branch_of=current_branch) -> str | None:
    words = strip_env(words)
    if not words:
        return None
    tool, args = words[0].rsplit("/", 1)[-1], words[1:]
    if tool == "git":
        return check_git(args, branch_of)
    if any(word.rsplit("/", 1)[-1] in DB_CLIENTS for word in words) and DESTRUCTIVE_SQL.search(full):
        return "DROP and TRUNCATE destroy data; a schema change goes through an Atlas migration."
    if tool == "atlas" and args[:2] == ["schema", "clean"]:
        return "`atlas schema clean` drops every object in the database."
    if tool == "docker":
        line = " ".join(args)
        if re.search(r"\bvolume (rm|prune)\b", line) or re.search(r"\bcompose\b.*\bdown\b.*(-v\b|--volumes)", line):
            return "Deleting Docker volumes deletes the database inside them."
        if re.search(r"\bsystem prune\b.*--volumes", line):
            return "Deleting Docker volumes deletes the database inside them."
    if tool == "gradlew" and "--stop" in args:
        return "`gradlew --stop` kills daemons that other builds on this machine use."
    if tool in ("gradlew", "gradle") and any("detektBaseline" in a for a in args):
        return "A baseline hides findings; the gate is zero findings. Fix them instead."
    if tool in ("pkill", "killall") and any(SHARED_PROCESSES.search(a) for a in args):
        return "Killing java, Gradle or Kotlin processes stops other builds on this machine."
    return None


def check_bash(command: str, branch_of=current_branch) -> str | None:
    for words in split_commands(command):
        reason = check_words(words, command, branch_of)
        if reason:
            return reason
    if re.search(r">>?\s*\S*baseline[^\s/]*\.xml", command, re.IGNORECASE):
        return "A baseline hides findings; the gate is zero findings. Fix them instead."
    return None


def check_tool(payload: dict) -> str | None:
    tool, data = payload.get("tool_name", ""), payload.get("tool_input") or {}
    if tool == "Bash":
        return check_bash(data.get("command", ""))
    if tool in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
        path = data.get("file_path") or data.get("notebook_path") or ""
        if BASELINE_FILE.search(path):
            return "A baseline hides findings; the gate is zero findings. Fix them instead."
    return None


def main() -> int:
    if len(sys.argv) >= 3 and sys.argv[1] == "--check":
        reason = check_bash(sys.argv[2])
        print(f"deny: {reason}" if reason else "allow")
        return 0
    try:
        reason = check_tool(json.load(sys.stdin))
    except Exception as error:  # noqa: BLE001 - a guard bug must fail open, never block a session
        print(f"agent_guard: skipped ({error})", file=sys.stderr)
        return 0
    if reason:
        print(json.dumps({
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": f"{reason} (tools/agent_guard.py; the owner runs it by hand if it is really meant)",
            }
        }))
    return 0


if __name__ == "__main__":
    sys.exit(main())
