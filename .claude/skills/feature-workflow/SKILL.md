---
name: feature-workflow
description: Takes one GitHub issue of this repository from spec to a merged pull request - worktree, spec, plan, subagents, the every-surface walk, the zero-findings gate, a fresh review, the pull request and the cleanup after the merge. Use when asked to build, implement or ship an issue or a feature here.
---

# Feature workflow

The repository's way to land a change. Every step names its evidence; a step without evidence is
not done.

## 1. Start from the issue

- Read the issue and the docs `AGENTS.md` routes it to. Unclear scope: ask now with the question
  tool, with a recommendation, never later in prose.
- One worktree per issue, never `main`:
  `git worktree add .agents/worktrees/<n>-<slug> -b feat/<n>-<slug> origin/main`.

## 2. Spec first

- A change a user sees gets its spec in `docs/features/<name>.md`: user stories, flows, edge cases,
  the reverse states (how to undo or leave it), what is out of scope.
- An architectural choice gets an ADR in `docs/architecture/decisions.md`, in the same pull request.

## 3. Plan, then delegate

- Split the work into tasks that each stand alone: the shared model and repository, the server
  endpoint, the UI, the tests.
- A task with a clear spec goes to a subagent with the whole brief (it sees nothing else): the
  issue, the worktree path, the files it owns, the rules from `AGENTS.md` it must keep, what to
  return. Choose its model and effort as `docs/agentic/README.md` (*Subagents*) says.
- The lead keeps the integration, the questions and the pull request.

## 4. Build with the project's skills

- `kotlin-project-feature-implementation` for placing the code, `kotlin-data-kmp-data-layer` for
  repositories and sync, `kotlin-ui-compose-multiplatform` for the UI, `kotlin-testing-kmp` for the
  tests.
- Test first where the behaviour is clear: a ViewModel test in `shared`, a Compose UI test in
  `composeApp/src/commonTest`, a resource test in `server`.

## 5. Prove it

- Walk *Hit every surface* in `AGENTS.md` and write down which entries applied.
- Run what changed the way a user would (desktop: `./gradlew :composeApp:run`, web:
  `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`, Android or iOS on a simulator) and keep a
  screenshot for the pull request.
- `./gradlew prePRcheck` once, clean. A finding gets fixed, never baselined.

## 6. Review and pull request

- A fresh reviewer that did not write the code reads the diff (`kotlin-kmp-code-review`), with
  the code, not the author's conclusions. Fix what holds up.
- Open the pull request with the template: `Closes #<n>`, the surfaces, the evidence, the gate.
- Watch the checks; fix a failure in the same branch. Merge when every check is green and every
  thread is resolved, never with `--admin`.

## 7. Clean up

- Fast-forward the main checkout to `origin/main`.
- Remove the worktree and the branch (`git worktree remove`, `git branch -D`, the remote branch).
- File what stayed open as a new issue, so nothing lives only in a chat.
