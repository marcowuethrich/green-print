# GreenPrint — Documentation

The map of the project's documentation: every fact has one home, and this page links to it.
Architecture diagrams use [Mermaid](https://mermaid.js.org/) with the [C4 model](https://c4model.com/).

## Architecture

| Document | Description |
|---|---|
| [Architecture Overview](architecture/overview.md) | C4 container diagram, offline-first sync, the write-through cache |
| [Architecture Decision Records](architecture/decisions.md) | Every decision with its context and consequences |
| [Testing Strategy](testing.md) | ViewModel, Compose UI and server tests; fakes; the gate |
| [Frontend Style Guide](style_guide.md) | Colours, typography, spacing, components and motion |

## Features

| Spec | Status |
|---|---|
| [Plant & Space Inventory](features/inventory.md) | Built |
| [Spatial Blueprint](features/spatial_blueprint.md) | Built |
| [Watering Reminders](features/notifications.md) | Next step, not built |

## Agentic engineering

| Document | Description |
|---|---|
| [The GreenPrint workflow](agentic/README.md) | How agents build this repository: instruction layers, the loop, subagents, hooks, gates, CI |
