# Project skills

Skills load on demand: an agent sees only each skill's `description` until the task matches it,
so they cost almost no context while unused. Five of them were kept on purpose, the ones this
project's work actually triggers; more skills mean more descriptions competing for the agent's
attention.

| Skill | Use it for | Source |
|---|---|---|
| [`feature-workflow`](feature-workflow/SKILL.md) | an issue end to end: spec, plan, build, gate, pull request | this project |
| [`kotlin-project-feature-implementation`](kotlin-project-feature-implementation/SKILL.md) | writing a KMP feature in the right layer and source set | third party ¹ |
| [`kotlin-data-kmp-data-layer`](kotlin-data-kmp-data-layer/SKILL.md) | repositories, the source of truth, sync and errors | third party ¹ |
| [`kotlin-ui-compose-multiplatform`](kotlin-ui-compose-multiplatform/SKILL.md) | shared Compose UI, state hoisting, modifiers | third party ¹ |
| [`kotlin-testing-kmp`](kotlin-testing-kmp/SKILL.md) | unit, ViewModel and Compose UI tests | third party ¹ |
| [`kotlin-kmp-code-review`](kotlin-kmp-code-review/SKILL.md) | a critical review of a diff before the pull request | third party ¹ |

¹ From [mmiani/kotlin-kmp-claude-agent-skills](https://github.com/mmiani/kotlin-kmp-claude-agent-skills),
Copyright 2026 Mariano Miani, under the Apache License 2.0 ([`LICENSE-APACHE-2.0.txt`](LICENSE-APACHE-2.0.txt)).
Modified here: cross-references to skills this repository does not ship were removed or
redirected, and the review skill's `name` matches its folder.
