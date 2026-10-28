# Feature Specification: Move-Money Payment Rails Modernization (`XFERPROC.cbl` to Java Spring Boot)

**Feature Branch**: `001-move-money-transfer`

**Created**: 2026-10-27

**Status**: Approved for Planning

**Input**: User description: "Modernize legacy mainframe COBOL payment movement program (`legacy-mainframe/XFERPROC.cbl`) into a resilient Java 21 / Spring Boot 3 microservice supporting Internal Brokerage-to-Bank transfers, Retirement-to-Brokerage transfers, ACH, and Wires with 100% functional parity, payment idempotency, Kafka event streaming, and multi-agent human-in-the-loop governance."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Core "Move Money" Transfer & Fee Execution (Priority: P1)

As an account holder or upstream banking channel, I need to execute Internal Transfers (`INT-BRKBNK`, `INT-RETBRK`), `ACH` transfers, and `WIRE` transfers with exact legacy mainframe fee calculations and balance checks so that money moves accurately without floating-point rounding errors.

**Why this priority**: Core payment movement and 100% dollar-and-cent parity with `XFERPROC.cbl` is the foundational business objective of the modernization initiative.

**Independent Test**: Can be fully tested by submitting transfer requests across all four transfer types (`INT-BRKBNK`, `INT-RETBRK`, `ACH`, `WIRE`) and verifying that calculated fees, debited balances, and status codes (`00` Completed, `10` Insufficient Funds, `30` Inactive Account) match `XFERPROC.cbl` 100%.

**Acceptance Scenarios**:

1. **Given** an active brokerage account (`isActive = true`) with an available balance of `$5,000.00`, **When** an internal Brokerage-to-Bank (`INT-BRKBNK`) or `ACH` transfer of `$500.00` is submitted, **Then** the fee MUST be `$0.00`, the new available balance MUST be `$4,500.00`, and the status code MUST be `00` (`COMPLETED`).
2. **Given** an active account with an available balance of `$5,000.00`, **When** a `WIRE` transfer of `$1,000.00` (under the `$10,000.00` fee-waiver threshold) is submitted, **Then** a `$25.00` standard wire fee MUST be applied (`totalDebit = $1,025.00`), the new balance MUST be `$3,975.00`, and the status code MUST be `00` (`COMPLETED`).
3. **Given** an active account with an available balance of `$25,000.00`, **When** a `WIRE` transfer of `$10,000.00` (at or above the `$10,000.00` fee-waiver threshold) is submitted, **Then** the wire fee MUST be waived (`$0.00`), `totalDebit` MUST be `$10,000.00`, the new balance MUST be `$15,000.00`, and the status code MUST be `00` (`COMPLETED`).
4. **Given** an active account with an available balance of `$1,000.00`, **When** a `WIRE` transfer of `$1,000.00` is submitted (`totalDebit = $1,025.00` including the `$25.00` wire fee), **Then** the transfer MUST be rejected with status code `10` (`REJECTED_INSUFFICIENT_FUNDS`) and the account balance MUST remain `$1,000.00`.

---

### User Story 2 - Payment Idempotency & Duplicate Prevention (Priority: P1)

As a payment platform engineer, I need every transfer request to enforce strict idempotency via a unique `Idempotency-Key` so that network retries or duplicate client submissions never debit a customer's account twice.

**Why this priority**: Duplicate financial debits are an unacceptable failure in a payment platform. Idempotency is required before any retry policy can safely operate.

**Independent Test**: Can be tested independently by sending the same transfer request twice with the identical `Idempotency-Key` and verifying that the account balance is only debited once.

**Acceptance Scenarios**:

1. **Given** a transfer request with `Idempotency-Key: "idem-tx-1001"` for `$500.00` has already succeeded on an account with a `$2,000.00` initial balance (leaving `$1,500.00`), **When** a network retry submits the same `Idempotency-Key: "idem-tx-1001"` for `$500.00`, **Then** the system MUST NOT debit the account again (balance remains `$1,500.00`) and MUST return legacy status code `90` (`DUPLICATE_IGNORED`) along with the original transaction record.

---

### User Story 3 - High-Value Fraud Gate, Soft-Delete & Immutable Audit Trail (Priority: P2)

As a compliance and risk officer, I need transfers exceeding the `$50,000.00` daily limit to be held for manual fraud review, inactive (soft-deleted) accounts to be blocked, and every transfer attempt recorded in an append-only audit log with masked account numbers.

**Why this priority**: Enforces regulatory compliance, fraud prevention (`XFERPROC.cbl` Rule 5), and Constitution Principles V (Encapsulation) and VII (Data Preservation).

**Independent Test**: Can be tested by submitting a transfer of `$50,000.01`, submitting a transfer on an account with `isActive = false`, and inspecting the immutable audit trail.

**Acceptance Scenarios**:

1. **Given** an active account with an available balance of `$100,000.00`, **When** a transfer of `$50,000.01` is submitted, **Then** the system MUST NOT auto-debit the balance (remains `$100,000.00`), MUST return status code `20` (`FLAGGED_FOR_FRAUD_REVIEW`), and MUST publish a fraud-review event.
2. **Given** a soft-deleted account (`isActive = false`) with a balance of `$5,000.00`, **When** any transfer is requested, **Then** the system MUST reject the transfer with status code `30` (`REJECTED_ACCOUNT_INACTIVE`) and write an immutable audit entry with masked account numbers (`****6789`).

---

### User Story 4 - Downstream Resiliency & Event-Driven Kafka Durability (Priority: P2)

As a reliability engineer, I need downstream settlement calls to use bounded retries with exponential backoff and a safe fallback state, and all state transitions published to Apache Kafka so offline consumers never lose payment events.

**Why this priority**: Directly satisfies the Resiliency Engineering and Kafka event-driven modernization requirements (Constitution Principles IV & VIII).

**Independent Test**: Can be tested by simulating a transient downstream failure (recovering on retry #2) and a persistent downstream outage (triggering the circuit breaker fallback `PENDING_SETTLEMENT_RETRY` and structured JSON error log).

**Acceptance Scenarios**:

1. **Given** the downstream settlement gateway experiences a temporary 1-second network blip, **When** a valid transfer is submitted, **Then** the service MUST retry with exponential backoff and complete the transfer without surfacing an error to the caller.
2. **Given** the downstream settlement gateway is completely unreachable across all retry attempts, **When** a valid transfer is submitted, **Then** the service MUST log a standardized JSON error (`{ timestamp, source, transactionId, error, context }`) via `ErrorHandlingService` and return the safe fallback state `PENDING_SETTLEMENT_RETRY` rather than crashing or corrupting state.

---

### Edge Cases

- **Boundary Condition — Exact `$10,000.00` Wire Fee Waiver**: A `WIRE` of `$9,999.99` incurs the `$25.00` fee (`totalDebit = $10,024.99`), whereas a `WIRE` of exactly `$10,000.00` waives the fee (`totalDebit = $10,000.00`), matching `IF WS-XFER-AMOUNT < WS-WIRE-FEE-WAIVE-LIMIT` in `XFERPROC.cbl`.
- **Boundary Condition — Exact `$50,000.00` Daily Fraud Threshold**: A transfer of exactly `$50,000.00` is processed normally (`00`), whereas `$50,000.01` triggers status `20` (`FLAGGED_FOR_FRAUD_REVIEW`), matching `IF WS-XFER-AMOUNT > WS-DAILY-FRAUD-LIMIT` in `XFERPROC.cbl`.
- **Boundary Condition — Wire Fee Pushes Debit Over Available Balance**: An account with `$1,000.00` attempting a `$1,000.00` `WIRE` fails with `10` (`REJECTED_INSUFFICIENT_FUNDS`) because `totalDebit` is `$1,025.00`.
- **Error Scenario — Downstream Consumer Offline**: Because every transfer state transition is published to the Kafka topic `move-money.transfers.v1` with durable retention, an offline notification or reporting service can replay missed messages from its last committed offset upon recovery.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose a versioned REST endpoint (`POST /api/v1/transfers`) requiring an `Idempotency-Key` header and validated transfer payload.
- **FR-002**: System MUST perform all monetary calculations using `java.math.BigDecimal` with `RoundingMode.HALF_UP` (scale `2`) to maintain 100% currency parity with COBOL `COMP-3` packed-decimal fields.
- **FR-003**: System MUST prevent duplicate transaction execution by checking `Idempotency-Key` prior to any balance modification and returning status code `90` (`DUPLICATE_IGNORED`) for repeated keys.
- **FR-004**: System MUST enforce soft-delete data preservation (`isActive` flag) and reject transfers on inactive accounts with status code `30` (`REJECTED_ACCOUNT_INACTIVE`).
- **FR-005**: System MUST apply a `$0.00` fee for `INT-BRKBNK`, `INT-RETBRK`, and `ACH` transfers, a `$25.00` fee for `WIRE` transfers under `$10,000.00`, and a `$0.00` waived fee for `WIRE` transfers of `$10,000.00` or more.
- **FR-006**: System MUST reject transfers where `availableBalance < (amount + feeAmount)` with status code `10` (`REJECTED_INSUFFICIENT_FUNDS`).
- **FR-007**: System MUST flag transfers where `amount > $50,000.00` with status code `20` (`FLAGGED_FOR_FRAUD_REVIEW`) without deducting funds until manual compliance approval.
- **FR-008**: System MUST write an append-only, immutable audit log entry and publish a domain event to Kafka topic `move-money.transfers.v1` for every processed transfer, masking raw account numbers as `****1234`.
- **FR-009**: System MUST implement bounded retries with exponential backoff, a safe fallback state (`PENDING_SETTLEMENT_RETRY`), and centralized structured JSON error logging (`{ timestamp, source, transactionId, error, context }`).

### Key Entities

- **`Account`**: Represents a customer Brokerage, Bank, or Retirement account (`accountId`, encapsulated `accountNumber` with masked getter, `availableBalance` as `BigDecimal`, `isActive` soft-delete flag).
- **`TransferRequest`**: Incoming payment instruction (`idempotencyKey`, `transactionId`, `sourceAccountNumber`, `targetAccountNumber`, `transferType`, `amount`).
- **`TransferResult`**: Outcome of the payment instruction (`transactionId`, `idempotencyKey`, `statusCode`, `status`, `amount`, `feeAmount`, `totalDebit`, `remainingBalance`, `maskedSourceAccount`, `maskedTargetAccount`, `timestamp`).
- **`AuditLogEntry`**: Immutable compliance record (`auditId`, `transactionId`, `action`, `statusCode`, `maskedSourceAccount`, `maskedTargetAccount`, `amount`, `feeAmount`, `timestamp`).
- **`TransferEvent`**: Kafka domain event published to `move-money.transfers.v1` (`eventId`, `transactionId`, `idempotencyKey`, `transferType`, `status`, `totalDebit`, `timestamp`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **100% Mainframe Input/Output Parity** — Automated JUnit 5 parity tests pass 100% of test cases across all 6 legacy `XFERPROC.cbl` rules and boundary conditions (`00`, `10`, `20`, `30`, `90`).
- **SC-002**: **Zero Duplicate Debits** — 100% of replayed requests sharing an `Idempotency-Key` return the original transaction result with zero secondary balance deductions.
- **SC-003**: **100% Safe Failure Recovery** — Simulated transient and persistent downstream outages result in zero unhandled exceptions and 100% structured JSON error logging with `PENDING_SETTLEMENT_RETRY` fallback.
- **SC-004**: **Zero Exposed Account Numbers** — 100% of API responses, audit records, and Kafka events mask account numbers to the last 4 digits (`****6789`).

## Assumptions

- **Authentication & Authorization**: Upstream API Gateway validates OAuth2/JWT bearer tokens and passes authenticated caller context to `/api/v1/transfers`.
- **Storage & Messaging**: The service uses decoupled repository and event-publisher interfaces so it can execute fast, zero-dependency JUnit parity tests in memory while binding to SQL Server / PostgreSQL and Apache Kafka in Pivotal Cloud Foundry (PCF) environments.
- **Human-in-the-Loop Governance**: No architectural plan (`plan.md`), task list (`tasks.md`), or Java code (`src/`) will be generated until this specification (`spec.md`) is reviewed and merged via Pull Request.
