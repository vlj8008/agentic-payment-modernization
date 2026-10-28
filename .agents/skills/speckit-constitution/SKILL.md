---
name: "speckit-constitution"
description: "Create or update the project constitution from interactive or provided principle inputs, ensuring all dependent templates stay in sync."
compatibility: "Requires spec-kit project structure with .specify/ directory"
metadata:
  author: "github-spec-kit"
  source: "templates/commands/constitution.md"
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Pre-Execution Checks

**Check for extension hooks (before constitution update)**:
- Check if `.specify/extensions.yml` exists in the project root.
- If it exists, read it and look for entries under the `hooks.before_constitution` key.
- If the YAML cannot be parsed or is invalid, skip hook checking silently and continue normally.
- Filter out hooks where `enabled` is explicitly `false`. Treat hooks without an `enabled` field as enabled by default.

## Outline

You are updating the project constitution at `.specify/memory/constitution.md`. Your job is to (a) collect/derive concrete values, (b) fill or update the constitution principles precisely, and (c) propagate any amendments across dependent artifacts (`spec.md`, `plan.md`, `tasks.md`).

Follow this execution flow:

1. Load the existing constitution at `.specify/memory/constitution.md`.
2. Collect/derive values for principles and governance dates:
   - `CONSTITUTION_VERSION` must increment according to semantic versioning rules (`MAJOR.MINOR.PATCH`).
3. Draft the updated constitution content:
   - Ensure each Principle section has a succinct name line and declarative, testable `MUST` rules.
   - Ensure the Governance section lists the amendment procedure, versioning policy, and compliance review expectations.
4. Consistency propagation checklist:
   - Verify that `plan.md`, `spec.md`, and `tasks.md` align with the updated principles and include a "Constitution Check" gate.
5. Prepend a Sync Impact Report as an HTML comment at the top of `.specify/memory/constitution.md`.
6. Halt and display `[WAITING FOR MANUAL VERIFICATION]` per Principle VI so the human maintainer can review and approve the updated constitution.
