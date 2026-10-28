# Agentic Payment-Rails Modernization Orchestrator (`GEMINI.md`)

## Purpose
This repository uses a **Spec-Driven Development (`spec-kit`) + Multi-Agent Modernization Pipeline** to migrate a legacy mainframe payment program ([`legacy-mainframe/XFERPROC.cbl`](legacy-mainframe/XFERPROC.cbl)) into a resilient **Java 21 / Spring Boot 3** microservice.

Every agent in this workspace MUST obey the governance rules in [`.specify/memory/constitution.md`](.specify/memory/constitution.md).

## Spec-Kit Artifacts (read these before doing any work)
| Order | File | Purpose |
| :--- | :--- | :--- |
| 1 | [`.specify/memory/constitution.md`](.specify/memory/constitution.md) | Non-negotiable engineering and AI-governance rules (Principles I-VIII) |
| 2 | [`specs/001-move-money-transfer/spec.md`](specs/001-move-money-transfer/spec.md) | WHAT to build: user stories, acceptance scenarios, requirements |
| 3 | [`specs/001-move-money-transfer/plan.md`](specs/001-move-money-transfer/plan.md) | HOW to build it: architecture and Constitution Check |
| 4 | [`specs/001-move-money-transfer/tasks.md`](specs/001-move-money-transfer/tasks.md) | Ordered checklist (T001-T027) to execute |
| - | [`specs/001-move-money-transfer/dependency-map.md`](specs/001-move-money-transfer/dependency-map.md) | Legacy COBOL fields, tables and queues mapped to Java |

## Human-in-the-Loop Rule (Constitution Principle VI)
For every task, the agent MUST:
1. Explain its reasoning and outline the exact steps before changing any file.
2. Implement the change and run the matching tests.
3. Run a build check.
4. Stop and display `[WAITING FOR MANUAL VERIFICATION]`. The human reviews the Pull Request and merges it before the next task starts.

Work is delivered in small Pull Requests, one theme per PR, each merged by the human reviewer.

## The 5 Specialized Modernization Agents (`.agents/skills/`)
| Order | Agent Skill | Responsibility | Output |
| :--- | :--- | :--- | :--- |
| 1 | `1-code-dependency-analyzer` | Reads the legacy COBOL and maps fields, tables, queues and status codes | `specs/001-move-money-transfer/dependency-map.md` |
| 2 | `2-requirements-extractor` | Turns the COBOL business rules into testable requirements | `specs/001-move-money-transfer/spec.md` |
| 3 | `3-architecture-planner` | Designs the Java / Spring Boot blueprint and runs the Constitution Check | `specs/001-move-money-transfer/plan.md` |
| 4 | `4-task-orchestrator` | Breaks the plan into an ordered task list | `specs/001-move-money-transfer/tasks.md` |
| 5 | `5-java-converter-validator` | Writes the Java code and JUnit parity tests that prove COBOL equivalence | `src/main/java/...` and `src/test/java/...` |

The skill files are added in a later Pull Request (tasks T002-T006).
