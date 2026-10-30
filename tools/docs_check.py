#!/usr/bin/env python3
"""The docs and agent-files gate:  python3 tools/docs_check.py

Agents follow the repository's text, so the text gets a gate like the code does. It fails when:

  1. AGENTS.md or CLAUDE.md grows past its line budget (long instruction files are followed less
     well; cut text before adding a rule);
  2. a relative link in a tracked Markdown file points to a file that does not exist;
  3. a document under docs/ is missing from the map in docs/README.md;
  4. a skill under .claude/skills/ lacks a `name` that matches its folder, or a `description`;
  5. a tracked file carries an AI attribution line.

Exit 0 when clean; otherwise one line per finding and exit 1.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BUDGETS = {"AGENTS.md": 100, "CLAUDE.md": 20}
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
ATTRIBUTION = re.compile("Co-Authored-By: " + "Claude|Generated with " + r"\[?Claude Code", re.IGNORECASE)


def tracked(pattern: str = "") -> list[Path]:
    args = ["git", "ls-files", "--cached", "--others", "--exclude-standard"]
    out = subprocess.run(args + ([pattern] if pattern else []), cwd=ROOT, capture_output=True, text=True, check=True)
    return [ROOT / line for line in out.stdout.splitlines() if (ROOT / line).is_file()]


def check_budgets() -> list[str]:
    findings = []
    for name, budget in BUDGETS.items():
        lines = len((ROOT / name).read_text().splitlines())
        if lines > budget:
            findings.append(f"{name}: {lines} lines, budget {budget}")
    return findings


def check_links(markdown: list[Path]) -> list[str]:
    findings = []
    for doc in markdown:
        text = re.sub(r"```.*?```", "", doc.read_text(), flags=re.DOTALL)
        for target in LINK.findall(text):
            if re.match(r"^(https?:|mailto:|#)", target):
                continue
            path = (doc.parent / target.split("#")[0]).resolve()
            if not path.exists():
                findings.append(f"{doc.relative_to(ROOT)}: broken link to {target}")
    return findings


def check_docs_map(markdown: list[Path]) -> list[str]:
    docs = ROOT / "docs"
    index = (docs / "README.md").read_text()
    linked = {(docs / target.split("#")[0]).resolve() for target in LINK.findall(index)}
    return [
        f"docs/README.md: {doc.relative_to(ROOT)} is not in the documentation map"
        for doc in markdown
        if doc.is_relative_to(docs) and doc != docs / "README.md" and doc.resolve() not in linked
    ]


def check_skills() -> list[str]:
    findings = []
    for skill in sorted((ROOT / ".claude" / "skills").glob("*/SKILL.md")):
        head = skill.read_text().split("---")[1] if skill.read_text().startswith("---") else ""
        name = re.search(r"^name:\s*(\S+)", head, re.MULTILINE)
        if not name or name.group(1) != skill.parent.name:
            findings.append(f"{skill.relative_to(ROOT)}: `name` must be {skill.parent.name}")
        if not re.search(r"^description:\s*\S", head, re.MULTILINE):
            findings.append(f"{skill.relative_to(ROOT)}: no `description`")
    return findings


def check_attribution(files: list[Path]) -> list[str]:
    findings = []
    for path in files:
        try:
            text = path.read_text()
        except (UnicodeDecodeError, OSError):
            continue
        if ATTRIBUTION.search(text):
            findings.append(f"{path.relative_to(ROOT)}: AI attribution line")
    return findings


def main() -> int:
    markdown = [p for p in tracked("*.md") if "/build/" not in str(p)]
    findings = (
        check_budgets()
        + check_links(markdown)
        + check_docs_map(markdown)
        + check_skills()
        + check_attribution(tracked())
    )
    for finding in findings:
        print(finding)
    print(f"docs_check: {len(findings)} finding(s)" if findings else "docs_check: clean")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
