# Agent rules for GreenPrint

A map and the rules an agent must not get wrong. Facts live in the docs it links (one fact, one
home); this file stays short on purpose (`tools/docs_check.py` holds it under its line budget),
because long instruction files are followed less well. Claude Code, Codex and Gemini CLI all read
this file (`CLAUDE.md` imports it, `.gemini/settings.json` names it).

## Read before you code

Always: `README.md` and `docs/README.md` (the documentation map). Then **only what the task touches**:

| If the task touches | Read |
|---|---|
| a feature a user sees | its spec in `docs/features/` (write or extend it first; never invent one speculatively) |
| persistence, sync, the API | `docs/architecture/overview.md` (offline-first, write-through cache), ADR-002, ADR-003 |
| UI | `docs/style_guide.md`, ADR-012 (strings), ADR-013 (focus) |
| tests | `docs/testing.md` |
| a decision, a new dependency, a module | `docs/architecture/decisions.md` |
| how agents work here (skills, hooks, gates) | `docs/agentic/README.md` |

**Conflict rule.** A change that conflicts with an ADR: state the conflict and ask before going on.
A decision that changes the architecture gets a new ADR in the same pull request.

## Workflow

1. **Research.** Map the code, check assumptions, reproduce a reported bug before fixing it.
2. **Strategy.** A short plan; a written proposal when it is architectural.
3. **Execution.** Plan, act, validate per sub-task. **Done means run:** the tests pass, the gate is
   clean, and you used what changed the way a user would (the app on a device, simulator, desktop
   or browser) and show the evidence (a screenshot, the output).

- **Decide or ask, never dangle.** No menu of options at the end of a reply: ask with the question
  tool, or proceed on your recommendation and say so.
- **Work comes from issues.** One issue, one branch, one worktree (`.agents/worktrees/<slug>`), one
  pull request that closes it. Never commit to `main`.
- **Delegate with intent.** A lead agent plans, hands self-contained tasks to subagents, integrates
  and reports. Pick each subagent's model and effort by the task's hardest step
  (`docs/agentic/README.md`, *Subagents*).

## Hit every surface

Before calling a change done, walk this list and say which entries applied:
- **Platforms:** Android, iOS, Desktop (JVM), Web (Wasm); the server when the API changes.
- **Offline:** works without the server, syncs when it comes back (write-through cache).
- **Reverse states:** a way in needs a way out (create, edit, archive, delete, undo).
- **Accessibility:** content descriptions, keyboard focus order, touch targets.
- **Strings:** no hard-coded user-facing text (ADR-012).
- **Docs:** fix what the change makes wrong, in that fact's one home.

## Gates (zero findings, no baseline)

- `./gradlew prePRcheck` runs once before a pull request opens: ktlint, detekt, the shared,
  Compose UI and server tests, the Android and Wasm builds. A finding gets fixed; a suppression
  only at its line with the reason beside it, never a baseline or a raised threshold.
- A docs or agent-files-only change runs `python3 tools/docs_check.py` instead.
- Merge only when every check is green and every review thread is resolved; never `--admin`.

## Guardrails

- `tools/agent_guard.py` (a Claude Code `PreToolUse` hook) refuses destructive commands: pushes to
  `main`, force pushes, `--no-verify`, `git reset --hard`, dropping or cleaning a database. A refusal
  is the owner's to run by hand, never worked around.
- No secrets in the repo, in prompts or in logs. Credentials come from a vault at run time.
- No AI attribution in commits, pull requests or comments.
