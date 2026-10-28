# Implementation Plan: Move-Money Payment Rails Modernization (`XFERPROC.cbl` to Java Spring Boot)

**Branch**: `001-move-money-transfer` | **Date**: 2026-10-27 | **Spec**: [`spec.md`](spec.md)

**Input**: Feature specification from `/specs/001-move-money-transfer/spec.md`

**Note**: This plan is governed by [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md) (`v1.1.0`).

## Summary

Modernize the legacy mainframe COBOL payment movement program ([`legacy-mainframe/XFERPROC.cbl`](../../legacy-mainframe/XFERPROC.cbl)) into a fault-tolerant **Java 21 / Spring Boot 3** microservice supporting Internal Transfers (`INT-BRKBNK`, `INT-RETBRK`), `ACH`, and `WIRE` payment rails. The implementation uses a 5-stage multi-agent modernization pipeline (`.agents/skills/`), strict `BigDecimal` (`HALF_UP`) currency math, `Idempotency-Key` duplicate prevention, bounded Exponential Backoff retries with Circuit Breaker fallback (`PENDING_SETTLEMENT_RETRY`), immutable soft-delete audit logging, and a decoupled **In-Memory Data Store** behind repository and event-publisher interfaces so no external database connection is required for normal operation or JUnit parity testing.

## Technical Context

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.3.x (`spring-boot-starter-web`, `spring-boot-starter-validation`)

**Storage**: Seeded **In-Memory Data Store** (`ConcurrentHashMap` state behind decoupled `AccountRepository`, `TransferRepository`, and `AuditLogRepository` interfaces). **No external database connection is required** for normal operation or JUnit parity testing, while allowing future integration with SQL Server or PostgreSQL without modifying business logic (SOLID Dependency Inversion).

**Testing**: JUnit 5 (`jupiter`) — 100% legacy mainframe input/output parity test suite (`TransferServiceParityTest`)

**Target Platform**: Local JVM / Pivotal Cloud Foundry (PCF) runtime with GitHub Actions CI/CD

**Project Type**: Backend Microservice (`web-service`) + Multi-Agent Modernization Skills (`.agents/skills/`)

**Performance Goals**: Sub-50ms in-memory transfer validation and execution; 100% deterministic parity test execution

**Constraints**: Zero floating-point currency math (`BigDecimal` scale `2`, `RoundingMode.HALF_UP` only); zero duplicate customer debits (`Idempotency-Key` required); zero hard deletes (`isActive` soft-delete flag); masked account numbers (`****6789`) in all outputs and logs; no external database required

**Scale/Scope**: Single bounded-context microservice (`move-money-transfer-service`) migrating 6 core mainframe rules and 5 legacy status codes (`00`, `10`, `20`, `30`, `90`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Requirement | Design Compliance Status |
| :--- | :--- | :--- |
| **I. Clean Spring Boot Architecture & SOLID** | Separate `@RestController`, `@Service`, and `@Repository` layers using decoupled interfaces and constructor injection. | ✅ **PASS** — `TransferController` delegates to `TransferService` interface, backed by `AccountRepository`, `TransferRepository`, and `AuditLogRepository` interfaces with in-memory implementations (`InMemoryAccountRepository`, etc.). |
| **II. Legacy Mainframe Functional & Currency Parity** | Use `BigDecimal` (`HALF_UP`, scale `2`) and preserve all 6 rules and status codes (`00`, `10`, `20`, `30`, `90`) from `XFERPROC.cbl`. | ✅ **PASS** — All monetary amounts use `BigDecimal` scale `2`; `TransferServiceParityTest` asserts exact parity on all boundary cases (`$10,000.00` wire fee waiver, `$50,000.00` fraud threshold). |
| **III. Payment Idempotency & Duplicate Prevention** | Require `Idempotency-Key` header and return original transaction (`90` / `DUPLICATE_IGNORED`) on replay without re-debiting. | ✅ **PASS** — `TransferRepository.findByIdempotencyKey()` executes as Step 1 prior to any balance mutation. |
| **IV. Event-Driven Durability (Kafka Outbox)** | Publish immutable domain events to topic `move-money.transfers.v1` on every state transition. | ✅ **PASS** — `TransferEventPublisher` interface publishes `TransferEvent` records to an in-memory Kafka topic outbox (`move-money.transfers.v1`) so event payloads and replay durability can be inspected and verified in tests without external broker setup. |
| **V. Enterprise API Security, Versioning & Encapsulation** | Versioned URI (`/api/v1/transfers`), `private` encapsulated account fields, and masked account numbers (`****1234`). | ✅ **PASS** — `Account` entity uses `private` fields with no public balance/account setters and exposes `getMaskedAccountNumber()`. |
| **VI. Iterative Development Cadence** | Explain reasoning, run tests/build, and halt with `[WAITING FOR MANUAL VERIFICATION]` before each PR merge. | ✅ **PASS** — Enforced across PR #1 (`constitution.md`), PR #2 (`spec.md`), PR #3 (`plan.md`), PR #4 (`tasks.md`), and PR #5 (`Implement`). |
| **VII. Data Preservation & Immutable Audit Trail** | Use `isActive` soft-delete flag (never hard delete) and write append-only audit records. | ✅ **PASS** — `Account.isActive` enforces soft deletion (`Status 30`), and `AuditLogRepository` only exposes `append()` (no delete/update methods). |
| **VIII. Resilience, Retries & Standardized Error Logging** | Exponential Backoff retries (max 3), Circuit Breaker fallback (`PENDING_SETTLEMENT_RETRY`), and JSON error structure `{ timestamp, source, transactionId, error, context }`. | ✅ **PASS** — Centralized in `ErrorHandlingService` and `SettlementGateway` retry policy. |

## Project Structure

### Documentation (this feature)

```text
specs/001-move-money-transfer/
├── spec.md              # Feature specification (/speckit-specify output)
├── plan.md              # This file (/speckit-plan output)
├── dependency-map.md    # Legacy COBOL to Java 21 data & storage mapping
└── tasks.md             # Ordered execution backlog (/speckit-tasks output)
```

### Source Code (repository root)

```text
.agents/
└── skills/                                          # 5 Specialized Multi-Agent Modernization Skills
    ├── 1-code-dependency-analyzer/SKILL.md
    ├── 2-requirements-extractor/SKILL.md
    ├── 3-architecture-planner/SKILL.md
    ├── 4-task-orchestrator/SKILL.md
    └── 5-java-converter-validator/SKILL.md

legacy-mainframe/
└── XFERPROC.cbl                                     # Legacy Mainframe COBOL Source Program

src/
├── main/java/com/movemoney/transfer/
│   ├── MoveMoneyTransferApplication.java            # Spring Boot 3 Entry Point
│   ├── controller/
│   │   └── TransferController.java                  # POST /api/v1/transfers (@RestController)
│   ├── model/
│   │   ├── Account.java                             # Encapsulated Account Entity (isActive, BigDecimal)
│   │   ├── AuditLogEntry.java                       # Immutable Compliance Audit Record
│   │   ├── TransferEvent.java                       # Kafka Domain Event (move-money.transfers.v1)
│   │   ├── TransferRequest.java                     # Validated Transfer Input Payload
│   │   ├── TransferResult.java                      # Standardized Transfer Output DTO
│   │   ├── TransferStatus.java                      # Enum (00, 10, 20, 30, 90, PENDING_SETTLEMENT_RETRY)
│   │   └── TransferType.java                        # Enum (INT_BRKBNK, INT_RETBRK, ACH, WIRE)
│   ├── repository/
│   │   ├── AccountRepository.java                   # Decoupled Account Data Access Interface
│   │   ├── InMemoryAccountRepository.java           # Seeded In-Memory Account Store (No DB Required)
│   │   ├── AuditLogRepository.java                  # Append-Only Audit Log Interface
│   │   ├── InMemoryAuditLogRepository.java          # In-Memory Append-Only Audit Store
│   │   ├── TransferRepository.java                  # Idempotency & Transfer Ledger Interface
│   │   └── InMemoryTransferRepository.java          # In-Memory Idempotency & Ledger Store
│   ├── messaging/
│   │   ├── SettlementGateway.java                   # Downstream Settlement Interface (Resiliency Target)
│   │   ├── TransferEventPublisher.java              # Event Publisher Interface (move-money.transfers.v1)
│   │   └── InMemoryKafkaEventPublisher.java         # In-Memory Kafka Outbox Topic Implementation
│   └── service/
│       ├── ErrorHandlingService.java                # Centralized Structured JSON Error Logger
│       ├── TransferService.java                     # Payment Business Logic Interface
│       └── TransferServiceImpl.java                 # Core Service (6 COBOL Rules + Retry/Circuit Breaker)
└── test/java/com/movemoney/transfer/
    └── TransferServiceParityTest.java               # JUnit 5 Suite Verifying 100% COBOL Parity & Resiliency
```

**Structure Decision**: Selected standard Maven / Spring Boot single-microservice layout (`src/main/java` and `src/test/java`) using decoupled interfaces backed by in-memory stores (`InMemoryAccountRepository`, `InMemoryTransferRepository`, `InMemoryAuditLogRepository`, `InMemoryKafkaEventPublisher`) so the service and JUnit 5 parity suite run standalone with zero external database or broker setup required.

## Complexity Tracking

| Pattern / Layer | Why Needed | Simpler Alternative Rejected Because |
|-----------------|------------|-------------------------------------|
| **5-Agent Pipeline (`.agents/skills/`)** | Separates COBOL dependency analysis, business rule extraction, architecture planning, task breakdown, and Java conversion into verifiable human-in-the-loop checkpoints. | Single-prompt AI conversion ("paste COBOL into Copilot") risks hallucinated fee math, missed fraud thresholds, and unverified architectural drift. |
| **Decoupled Interface + In-Memory Store Pattern** | Enables deterministic, zero-setup execution and sub-second JUnit 5 parity testing while supporting future SQL Server / PostgreSQL and Kafka adapters via Dependency Inversion. | Hard-coding an external database connection requires local infrastructure setup and violates SOLID Dependency Inversion (Principle I). |
