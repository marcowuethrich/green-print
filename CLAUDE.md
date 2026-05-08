# Architectural Rules & Documentation

Before writing any code, proposing architectural changes, or adding new dependencies, you MUST execute the following steps:

1. **Start Here:** Read the file `docs/README.md`. This file serves as the table of contents for the entire project's documentation and Architecture Decision Records (ADRs).
2. **Consult Diagrams:** Review the C4 diagrams in `docs/architecture/overview.md` to understand the system structure, specifically the **Offline-First SQLDelight strategy** and the **Write-Through Cache** pattern.
3. **Navigate:** Based on the current task, use the `docs/README.md` index to find and read the specific, relevant ADR markdown files.
4. **Comply:** Ensure your proposed solution strictly adheres to the established patterns and constraints detailed in those documents.
5. **Conflict Resolution:** If your proposed solution conflicts with an existing ADR, you must explicitly state the conflict and stop to ask for developer confirmation before proceeding.

## Agentic Engineering Workflow

To maintain the professional standards of this portfolio, all complex tasks MUST follow the **Research -> Strategy -> Execution** lifecycle:

- **Research:** Systematically map the codebase, validate all assumptions, and **empirically reproduce** any reported issues before proposing a fix.
- **Strategy:** Draft a concise design document or plan. If the change involves architectural decisions, provide a clear technical proposal before implementation.
- **Execution:** Follow an iterative **Plan -> Act -> Validate** cycle for each sub-task. Every implementation is incomplete without corresponding automated tests and validation against project linting/build standards.