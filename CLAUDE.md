@AGENTS.md

## Claude Code specifics

- Project skills live in `.claude/skills/` (`.claude/skills/README.md` lists them and where they
  come from). Start a feature with `feature-workflow`.
- `.claude/settings.json` wires `tools/agent_guard.py` as a `PreToolUse` hook; personal overrides
  go to `.claude/settings.local.json` (git-ignored).
- Questions for the developer go through `AskUserQuestion`, a plan for an architectural change
  through plan mode.
