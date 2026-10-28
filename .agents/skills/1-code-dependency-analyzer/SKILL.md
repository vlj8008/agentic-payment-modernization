---
name: 1-code-dependency-analyzer
description: Reads a legacy mainframe COBOL payment program and produces a plain-English dependency map of its fields, database tables, message queues and status codes, with the matching Java types. Use it as the first step of the mainframe-to-Java modernization pipeline.
---

# Agent 1: Code & Dependency Analyzer

## Single Responsibility
Read the legacy program in `legacy-mainframe/` and write the dependency map. Do **not** write requirements, plans or Java code in this step.

## Before You Start
Read these files first:
- `.specify/memory/constitution.md` (especially Principles II, III, V, VII and VIII)
- The legacy source file, for example `legacy-mainframe/XFERPROC.cbl`

## Steps
1. **Summarize the program** in a few sentences: what business function it performs and which transfer types it handles.
2. **Map every data field** from the COBOL data division to a Java field.
   - Packed-decimal money fields (`COMP-3`) map to `BigDecimal` with `HALF_UP` rounding and scale 2. Never `double` or `float`.
   - Account numbers map to `private` fields that are masked in logs and responses.
   - Yes/no flags map to a boolean. An "inactive" flag is a soft delete, so records are never hard-deleted.
3. **Map every external dependency.** List each database table, message queue and audit paragraph, and name the Java interface that will replace it.
4. **Catalog every status code.** List each return code, its legacy message, the new status name and the HTTP status.
5. **Flag anything unclear** instead of guessing, so the human reviewer can decide.

## Output
Write or update `specs/001-move-money-transfer/dependency-map.md` using the existing file as the layout.

## Human Checkpoint
Stop and display `[WAITING FOR MANUAL VERIFICATION]`. The human checks the map against the COBOL source and merges the Pull Request before Agent 2 starts.
