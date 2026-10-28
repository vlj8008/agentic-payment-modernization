# Legacy Mainframe Code & Dependency Analysis (`dependency-map.md`)
**Source Program**: [`legacy-mainframe/XFERPROC.cbl`](../../legacy-mainframe/XFERPROC.cbl)  
**Target Platform**: Java 21 / Spring Boot 3.x Microservice (Decoupled Interface + In-Memory Store Architecture)  
**Governance Reference**: [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md) (`v1.1.0`)

---

## 1. Program Summary
`XFERPROC.cbl` is the core legacy mainframe payment movement routine ("Move Money") responsible for validating and executing:
- **Internal Brokerage-to-Bank Transfers** (`INT-BRKBNK`)
- **Internal Retirement-to-Brokerage Transfers** (`INT-RETBRK`)
- **Automated Clearing House Transfers** (`ACH`)
- **Domestic/International Wire Transfers** (`WIRE`)

---

## 2. Data Division Field Mapping (COBOL → Java 21)

Per **Principle II (Legacy Mainframe Functional & Currency Parity)** and **Principle V (Encapsulation)**, all COBOL `COMP-3` packed-decimal currency fields MUST map to `java.math.BigDecimal` with `RoundingMode.HALF_UP` (scale `2`), and sensitive account numbers MUST be encapsulated and masked in logs.

| Legacy COBOL Field | Picture Clause | Legacy Type | Target Java 21 Field & Type | Governance Rule Applied |
| :--- | :--- | :--- | :--- | :--- |
| `WS-IDEMPOTENCY-KEY` | `PIC X(36)` | Alphanumeric | `String idempotencyKey` (UUID) | **Principle III**: Required header (`Idempotency-Key`) to prevent duplicate debits. |
| `WS-TRANSACTION-ID` | `PIC X(20)` | Alphanumeric | `String transactionId` | **Principle III & VII**: Unique transaction identifier for audit trail. |
| `WS-SOURCE-ACCT-NUM` | `PIC X(12)` | Alphanumeric | `private String sourceAccountNumber` | **Principle V**: Encapsulated; masked as `****1234` in logs/responses. |
| `WS-TARGET-ACCT-NUM` | `PIC X(12)` | Alphanumeric | `private String targetAccountNumber` | **Principle V**: Encapsulated; masked as `****5678` in logs/responses. |
| `WS-XFER-TYPE` | `PIC X(10)` | Alphanumeric | `TransferType transferType` (`Enum`) | Maps `INT-BRKBNK`, `INT-RETBRK`, `ACH`, `WIRE`. |
| `WS-XFER-AMOUNT` | `PIC 9(09)V99 COMP-3` | Packed Decimal | `BigDecimal amount` (scale 2, `HALF_UP`) | **Principle II**: Prohibits `double`/`float`. |
| `WS-ACCT-ACTIVE-FLAG` | `PIC X(01)` | `'Y'` / `'N'` Flag | `private boolean isActive` | **Principle VII**: Soft-delete flag (never hard-delete records). |
| `WS-AVAILABLE-BALANCE` | `PIC 9(09)V99 COMP-3` | Packed Decimal | `private BigDecimal availableBalance` | **Principle II & V**: `private` encapsulated balance with `BigDecimal`. |
| `WS-FEE-AMOUNT` | `PIC 9(05)V99 COMP-3` | Packed Decimal | `BigDecimal feeAmount` | **Principle II**: `$0.00` or `$25.00` wire fee. |
| `WS-TOTAL-DEBIT` | `PIC 9(09)V99 COMP-3` | Packed Decimal | `BigDecimal totalDebit` | `amount.add(feeAmount)` with `HALF_UP`. |

---

## 3. Infrastructure & Storage Mapping (Decoupled In-Memory Architecture)

| Legacy Mainframe Dependency | Legacy Role | Modernized Interface & In-Memory Store | Resiliency / Governance Pattern |
| :--- | :--- | :--- | :--- |
| **`DB2-ACCT-MASTER`** | Mainframe account table | `AccountRepository` → `InMemoryAccountRepository` | Soft-delete (`isActive`) check per **Principle VII**; zero external DB required. |
| **`DB2-XFER-LEDGER`** | Mainframe transfer & idempotency ledger | `TransferRepository` → `InMemoryTransferRepository` | Stores `idempotencyKey` + `transactionId` per **Principle III**. |
| **`DB2-AUDIT-LOG`** | Mainframe paragraph `8000-WRITE-IMMUTABLE-AUDIT-LOG` | `AuditLogRepository` → `InMemoryAuditLogRepository` | Append-only immutable audit trail per **Principle VII**. |
| **`MQ-SETTLEMENT-QUEUE`** | Mainframe paragraph `9000-PUBLISH-SETTLEMENT-EVENT` | `TransferEventPublisher` → `InMemoryKafkaEventPublisher` (`move-money.transfers.v1`) | Event-driven durability + Exponential Backoff & Circuit Breaker fallback (`PENDING_SETTLEMENT_RETRY`) per **Principles IV & VIII**. |

---

## 4. Legacy Return Status Code Catalog

| COBOL `WS-STATUS-CODE` | Legacy Message | Target Domain Status Enum | HTTP Status |
| :--- | :--- | :--- | :--- |
| **`00`** | `COMPLETED: TRANSFER PROCESSED SUCCESSFULLY` | `COMPLETED` | `200 OK` |
| **`10`** | `REJECTED: INSUFFICIENT AVAILABLE BALANCE` | `REJECTED_INSUFFICIENT_FUNDS` | `422 Unprocessable Entity` |
| **`20`** | `FLAGGED: EXCEEDS $50,000 LIMIT - MANUAL REVIEW` | `FLAGGED_FOR_FRAUD_REVIEW` | `202 Accepted` |
| **`30`** | `REJECTED: SOURCE OR TARGET ACCOUNT INACTIVE` | `REJECTED_ACCOUNT_INACTIVE` | `422 Unprocessable Entity` |
| **`90`** | `DUPLICATE: IDEMPOTENCY KEY ALREADY PROCESSED` | `DUPLICATE_IGNORED` (returns original result) | `200 OK` (Idempotent replay) |
| *(New Resiliency State)* | *(Downstream settlement transient outage)* | `PENDING_SETTLEMENT_RETRY` | `202 Accepted` (Safe fallback per **Principle VIII**) |
