# Tasks: Move-Money Payment Rails Modernization (`XFERPROC.cbl` to Java Spring Boot)

**Input**: Design documents from `/specs/001-move-money-transfer/`

**Prerequisites**: [`plan.md`](plan.md) (required), [`spec.md`](spec.md) (required for user stories), [`dependency-map.md`](dependency-map.md)

**Tests**: Automated JUnit 5 parity tests are MANDATORY per [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md) (Principle II & Testing Gates).

**Organization**: Tasks are grouped by foundational phase and user story (`US1`–`US4`) to enable independent implementation and testing of each requirement.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`US1`, `US2`, `US3`, `US4`)
- Exact file paths are included in all descriptions

---

## Phase 1: Setup (Multi-Agent Pipeline Skills & Maven Project Initialization)

**Purpose**: Initialize the 5 specialized Modernization Agent skills (`.agents/skills/`) and the Java 21 / Spring Boot 3 Maven project structure defined in `plan.md`.

- [ ] T001 Create multi-agent orchestrator guide in `GEMINI.md`
- [ ] T002 [P] Create Agent 1 skill in `.agents/skills/1-code-dependency-analyzer/SKILL.md`
- [ ] T003 [P] Create Agent 2 skill in `.agents/skills/2-requirements-extractor/SKILL.md`
- [ ] T004 [P] Create Agent 3 skill in `.agents/skills/3-architecture-planner/SKILL.md`
- [ ] T005 [P] Create Agent 4 skill in `.agents/skills/4-task-orchestrator/SKILL.md`
- [ ] T006 [P] Create Agent 5 skill in `.agents/skills/5-java-converter-validator/SKILL.md`
- [ ] T007 Initialize Java 21 / Spring Boot 3.3 Maven configuration in `pom.xml` and application entry point in `src/main/java/com/movemoney/transfer/MoveMoneyTransferApplication.java`

---

## Phase 2: Foundational (Domain Models, Decoupled Interfaces, In-Memory Stores & Error Handling)

**Purpose**: Core domain entities, decoupled repository/messaging interfaces, seeded in-memory data stores (no external DB required), and centralized structured JSON error logging that MUST be complete before user stories are wired.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T008 [P] Create `TransferType` (`INT_BRKBNK`, `INT_RETBRK`, `ACH`, `WIRE`) and `TransferStatus` (`00`, `10`, `20`, `30`, `90`, `PENDING_SETTLEMENT_RETRY`) enums in `src/main/java/com/movemoney/transfer/model/`
- [ ] T009 [P] Create encapsulated `Account` entity (`private` balance with `BigDecimal`, `private boolean isActive` soft-delete flag, and `getMaskedAccountNumber()`) in `src/main/java/com/movemoney/transfer/model/Account.java`
- [ ] T010 [P] Create `TransferRequest`, `TransferResult`, `AuditLogEntry`, and `TransferEvent` records in `src/main/java/com/movemoney/transfer/model/`
- [ ] T011 [P] Create decoupled `AccountRepository` interface and seeded `InMemoryAccountRepository` in `src/main/java/com/movemoney/transfer/repository/`
- [ ] T012 [P] Create decoupled `TransferRepository` interface and `InMemoryTransferRepository` in `src/main/java/com/movemoney/transfer/repository/`
- [ ] T013 [P] Create append-only `AuditLogRepository` interface and `InMemoryAuditLogRepository` in `src/main/java/com/movemoney/transfer/repository/`
- [ ] T014 [P] Create `SettlementGateway`, `TransferEventPublisher`, and `InMemoryKafkaEventPublisher` (`move-money.transfers.v1`) in `src/main/java/com/movemoney/transfer/messaging/`
- [ ] T015 Implement centralized `ErrorHandlingService` logging standardized JSON (`{ timestamp, source, transactionId, error, context }`) per Principle VIII in `src/main/java/com/movemoney/transfer/service/ErrorHandlingService.java`

**Checkpoint**: Foundation ready — User Story implementation and parity testing can now proceed.

---

## Phase 3: User Story 1 - Core "Move Money" Transfer & Fee Execution (Priority: P1) 🎯 MVP

**Goal**: Implement exact `XFERPROC.cbl` fee calculation (`$0.00` for Internal/ACH, `$25.00` for Wires `< $10,000.00`, `$0.00` waived for Wires `>= $10,000.00`), sufficient funds check (`Status 10`), and atomic balance debit (`Status 00`) using `BigDecimal` (`RoundingMode.HALF_UP`, scale `2`).

**Independent Test**: Verify `INT_BRKBNK`, `INT_RETBRK`, `ACH`, and `WIRE` transfers calculate exact fees and balances matching `XFERPROC.cbl`.

### Parity Tests for User Story 1 ⚠️

- [ ] T016 [P] [US1] Write JUnit 5 parity tests for Internal/ACH (`$0` fee), Wire `< $10,000` (`$25` fee), Wire `>= $10,000` (`$0` fee waived), and Insufficient Funds (`Status 10`) in `src/test/java/com/movemoney/transfer/TransferServiceParityTest.java`

### Implementation for User Story 1

- [ ] T017 [US1] Define `TransferService` interface in `src/main/java/com/movemoney/transfer/service/TransferService.java`
- [ ] T018 [US1] Implement `BigDecimal` (`HALF_UP`) fee schedule, sufficient balance verification (`Status 10`), and atomic debit (`Status 00`) in `src/main/java/com/movemoney/transfer/service/TransferServiceImpl.java`

**Checkpoint**: User Story 1 (`XFERPROC.cbl` Rules 3, 4 & 6) is functional and verified by JUnit 5 tests.

---

## Phase 4: User Story 2 - Payment Idempotency & Duplicate Prevention (Priority: P1)

**Goal**: Enforce `Idempotency-Key` verification (`XFERPROC.cbl` Rule 1 / `Status 90`) before any balance modification so network retries never double-charge a customer.

**Independent Test**: Submit two identical transfer requests with the same `Idempotency-Key` and assert the account balance is debited only once and the second call returns `90` (`DUPLICATE_IGNORED`).

### Parity Tests for User Story 2 ⚠️

- [ ] T019 [P] [US2] Add JUnit 5 idempotency replay test (`Status 90` / zero duplicate debit) in `src/test/java/com/movemoney/transfer/TransferServiceParityTest.java`

### Implementation for User Story 2

- [ ] T020 [US2] Implement `Idempotency-Key` lookup and replay handling (`Status 90` / `DUPLICATE_IGNORED`) in `src/main/java/com/movemoney/transfer/service/TransferServiceImpl.java`

**Checkpoint**: User Stories 1 and 2 work together with zero duplicate transaction risk.

---

## Phase 5: User Story 3 - High-Value Fraud Gate, Soft-Delete & Immutable Audit Trail (Priority: P2)

**Goal**: Enforce soft-delete account verification (`isActive == false` → `Status 30`), `$50,000.00` daily fraud limit flagging (`amount > $50,000.00` → `Status 20`), and append-only audit logging with masked account numbers (`****6789`).

**Independent Test**: Submit a transfer on an inactive account (`Status 30`) and a transfer of `$50,000.01` (`Status 20`), verifying zero balance deduction and masked audit entries.

### Parity Tests for User Story 3 ⚠️

- [ ] T021 [P] [US3] Add JUnit 5 tests for soft-deleted account rejection (`Status 30`), `$50,000.01` fraud gate (`Status 20`), and account masking (`****6789`) in `src/test/java/com/movemoney/transfer/TransferServiceParityTest.java`

### Implementation for User Story 3

- [ ] T022 [US3] Implement soft-delete check (`Status 30`), `$50,000.00` fraud threshold check (`Status 20`), and immutable `AuditLogRepository.append()` calls in `src/main/java/com/movemoney/transfer/service/TransferServiceImpl.java`

**Checkpoint**: All 6 legacy `XFERPROC.cbl` business rules (`00`, `10`, `20`, `30`, `90`) are complete and verified.

---

## Phase 6: User Story 4 - Downstream Resiliency & Event-Driven Kafka Durability (Priority: P2)

**Goal**: Add bounded Exponential Backoff retries (max 3 attempts), Circuit Breaker safe fallback (`PENDING_SETTLEMENT_RETRY`), standardized JSON error logging, and Kafka event publishing (`move-money.transfers.v1`).

**Independent Test**: Simulate a transient 1-blip settlement failure (succeeds on retry) and a persistent settlement outage (returns `PENDING_SETTLEMENT_RETRY` and logs structured JSON error).

### Parity & Resiliency Tests for User Story 4 ⚠️

- [ ] T023 [P] [US4] Add JUnit 5 tests for Exponential Backoff retry recovery, persistent outage fallback (`PENDING_SETTLEMENT_RETRY`), structured JSON error logging, and Kafka event publishing in `src/test/java/com/movemoney/transfer/TransferServiceParityTest.java`

### Implementation for User Story 4

- [ ] T024 [US4] Implement Exponential Backoff retry loop, safe fallback state (`PENDING_SETTLEMENT_RETRY`), `ErrorHandlingService` JSON logging, and `TransferEventPublisher` Kafka outbox calls in `src/main/java/com/movemoney/transfer/service/TransferServiceImpl.java`

**Checkpoint**: Resiliency and Kafka event-driven durability verified.

---

## Phase 7: Polish, REST API Controller & CI/CD Verification

**Purpose**: Expose the versioned enterprise REST endpoint (`POST /api/v1/transfers`) and verify the full JUnit 5 parity test suite.

- [ ] T025 Implement `TransferController` (`POST /api/v1/transfers` with `Idempotency-Key` header) in `src/main/java/com/movemoney/transfer/controller/TransferController.java`
- [ ] T026 [P] Create GitHub Actions CI workflow in `.github/workflows/ci.yml` (JDK 21, `mvn test`, triggered on pull requests and pushes to `main`)
- [ ] T027 Execute full JUnit 5 parity test suite, confirm the GitHub Actions CI check is green on the pull request, then halt with `[WAITING FOR MANUAL VERIFICATION]` per Principle VI

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — initializes multi-agent skills and Maven project.
- **Foundational (Phase 2)**: Depends on Phase 1 — **BLOCKS** all user stories (`US1`–`US4`).
- **User Stories (Phases 3–6)**: Depend on Phase 2 completion; ordered by priority (`P1` → `P2`).
- **Polish & REST API (Phase 7)**: Depends on Phases 3–6 completion.

### Parallel Opportunities

- Tasks marked **`[P]`** (`T002`–`T006` agent skills and `T008`–`T014` models/interfaces/in-memory stores) operate on independent files and can be created in parallel.

---

## Implementation Strategy

1. Complete **Phase 1 (Setup)** and **Phase 2 (Foundational)** to establish the 5 modernization agents, domain models, decoupled interfaces, and seeded in-memory stores.
2. Execute **Phases 3–6 (`US1`–`US4`)** to implement all 6 `XFERPROC.cbl` business rules, idempotency, fraud gating, soft-delete audit logging, exponential backoff retries, and Kafka event publishing.
3. Complete **Phase 7** by exposing `POST /api/v1/transfers`, running the automated JUnit 5 parity suite, and halting at `[WAITING FOR MANUAL VERIFICATION]` for human Pull Request review.
