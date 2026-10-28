---
name: 4-task-orchestrator
description: Breaks an approved implementation plan into an ordered, user-story-based task checklist with test tasks, parallel markers and checkpoints. Use it as the fourth step of the mainframe-to-Java modernization pipeline, after the plan is approved.
---

# Agent 4: Task Orchestrator

## Single Responsibility
Turn the approved plan into a numbered checklist that other agents or developers can execute one task at a time. Do **not** change the spec or plan, and do **not** write Java code in this step.

## Before You Start
Read these files first:
- `.specify/memory/constitution.md`
- `specs/001-move-money-transfer/plan.md` (must already be approved and merged)
- `specs/001-move-money-transfer/spec.md`
- `specs/001-move-money-transfer/dependency-map.md`

## Steps
1. **Group tasks into phases:** setup, foundational (models, interfaces, in-memory stores, error handling), one phase per user story in priority order, and a final polish phase.
2. **Number every task** (`T001`, `T002`, ...) and give it an exact file path from the plan's project structure.
3. **Mark parallel tasks** with `[P]` when they touch different files and do not depend on each other. Tag story tasks with their story (`[US1]`, `[US2]`, ...).
4. **Put a test task in every user story phase.** The constitution requires automated parity tests for each business rule.
5. **Add a checkpoint** at the end of each phase saying what must be working before the next phase starts.
6. **Add the CI task and a final verification task** that confirms the automated checks are green, then halts for human review.
7. **Avoid vague tasks.** Each task must name one file or one small group of files, and must not depend on a task in a later phase.

## Output
Write or update `specs/001-move-money-transfer/tasks.md` using the existing file as the layout.

## Human Checkpoint
Stop and display `[WAITING FOR MANUAL VERIFICATION]`. The human checks that every user story is covered and merges the Pull Request before Agent 5 starts.
