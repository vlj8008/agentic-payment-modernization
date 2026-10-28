<!--
Sync Impact Report:
- Version change: 1.0.0 -> 1.1.0
- Modified principles:
  - Principle I: Angular Standalone -> Clean Spring Boot Architecture & Single Responsibility
  - Principle II: Component Discipline -> Legacy Mainframe Functional & Currency Parity
  - Principle III: Service & DI Standards -> Payment Idempotency & Duplicate Prevention
  - Principle IV: Signal Forms -> Event-Driven Durability (Kafka Outbox Pattern)
  - Principle V: Accessibility & Strict Types -> Enterprise API Security, Versioning & Encapsulation
  - Principle VI: Iterative Development Cadence (retained from Pivot Constitution v1.1.0 with [WAITING FOR MANUAL VERIFICATION] gate)
  - Principle VII: Data Preservation (retained from Pivot Constitution v1.1.0 + Immutable Financial Audit Trail)
  - Principle VIII: Resilience & Error Handling (retained from Pivot Constitution v1.1.0 + Exponential Backoff Retries & Circuit Breakers)
- Added sections:
  - Multi-Agent Modernization Pipeline Governance
  - Decoupled Architecture and Storage Constraints (Mainframe DB2/VSAM -> PostgreSQL/SQL Server + Kafka)
- Templates requiring updates:
  - ✅ .specify/templates/spec-template.md (aligned with Mainframe Rule Extraction & Parity)
  - ✅ .specify/templates/plan-template.md (aligned with Constitution Check gates I-VIII)
  - ✅ .specify/templates/tasks-template.md (aligned with Iterative Cadence & Parity Testing)
- Follow-up TODOs: None
-->

# Move-Money Payment Modernization Constitution

## Core Principles

### I. Clean Spring Boot Architecture & Single Responsibility
All modernized backend services MUST follow a clean, layered Java 21 and Spring Boot 3+ architecture with strict Single Responsibility separation:
- **Controller Layer (`@RestController`)**: Handles HTTP routing, request validation (`@Valid`), and response mapping only. Business logic in controllers is strictly prohibited.
- **Service Layer (`@Service`)**: Encapsulates all payment domain rules, fee calculations, fraud limit checks, and orchestration.
- **Repository Layer (`@Repository`)**: Handles database persistence and query projection through decoupled interfaces (`TransferRepository`, `AuditLogRepository`).
- **Dependency Injection**: Services MUST depend on interfaces (abstractions) rather than concrete implementations and use constructor injection for clean testability and loose coupling.

### II. Legacy Mainframe Functional & Currency Parity
Converted Java microservices MUST preserve 100% business-rule and input/output parity with the legacy mainframe COBOL programs (`legacy-mainframe/XFERPROC.cbl`):
- Every monetary value (account balances, transfer amounts, wire fees, daily limits) MUST use `java.math.BigDecimal` with explicit `RoundingMode.HALF_UP` (scale of 2). Floating-point types (`double`, `float`) are strictly prohibited for financial calculations.
- All legacy mainframe status codes (`00` Approved, `10` Insufficient Funds, `20` Exceeds Daily Limit / Fraud Review, `30` Invalid Account / Inactive, `90` Duplicate Transaction) MUST map deterministically to standardized domain statuses and HTTP response codes.

### III. Payment Idempotency & Duplicate Prevention
In a financial payment platform, charging or transferring a customer's money twice is an unacceptable failure:
- Every state-changing payment endpoint (`POST /api/v1/transfers`) MUST require an `Idempotency-Key` request header and a unique `transactionId`.
- Before executing any balance debit or credit, the service MUST check the idempotency store to verify whether the `Idempotency-Key` has already been processed.
- If a duplicate request arrives (due to a client retry or network blip), the service MUST return the original transaction result without re-executing the transfer.

### IV. Event-Driven Durability (Kafka Outbox Pattern)
Synchronous HTTP calls alone cannot guarantee downstream delivery if a settlement or notification service goes offline:
- Every payment state transition (`COMPLETED`, `FLAGGED_FOR_FRAUD_REVIEW`, `REJECTED_INSUFFICIENT_FUNDS`, `PENDING_SETTLEMENT_RETRY`) MUST publish an immutable domain event to the enterprise Kafka topic (`move-money.transfers.v1`).
- Events MUST include a unique event ID, `transactionId`, `idempotencyKey`, transfer type (`INTERNAL_BROKERAGE_TO_BANK`, `INTERNAL_RETIREMENT_TO_BROKERAGE`, `ACH`, `WIRE`), timestamp, and status so offline consumers can replay events from their last committed offset when they come back online.

### V. Enterprise API Security, Versioning & Encapsulation
All exposed APIs MUST adhere to enterprise financial security standards:
- **API Versioning**: All endpoints MUST be explicitly versioned in the URI path (`/api/v1/transfers`) to prevent breaking existing API consumers.
- **Authentication & Authorization**: Endpoints MUST enforce token-based authentication (OAuth2 / JWT) and role-based authorization, ensuring a user can only initiate transfers from accounts they own.
- **Rate Limiting & Encryption**: APIs MUST enforce rate limiting to prevent abuse and require TLS encryption in transit.
- **Encapsulation of Sensitive Data**: Domain models MUST encapsulate sensitive fields (such as raw account numbers or tax identifiers) using `private` fields without public setters, and mask account numbers (e.g., `****6789`) in all logs and responses.

### VI. Iterative Development Cadence
Development and AI-assisted modernization MUST proceed in discrete, verifiable increments. For every task in the pipeline, the agent MUST:
1. Clearly explain its reasoning and outline the exact steps before executing any command or modifying any file.
2. Implement the artifact or code change and execute the corresponding unit and parity tests (`mvn test` or parity test suite).
3. Execute a compilation and build verification check to ensure the project dependency graph remains valid.
4. Only AFTER verification succeeds, the agent MUST halt execution, display `[WAITING FOR MANUAL VERIFICATION]`, and prompt the developer to review and approve before initiating the next task.

### VII. Data Preservation & Immutable Financial Audit Trail
Never permanently delete financial transactions, account records, or audit logs from the application:
- All domain models MUST use a soft-delete approach by toggling an `isActive` property to `false` rather than executing SQL `DELETE` statements.
- Every transfer attempt MUST write an append-only, immutable audit trail record capturing the before/after state, actor, timestamp, and reason code.

### VIII. Resilience, Retry Policies & Standardized Error Handling
The payment service MUST be resilient to transient network and downstream infrastructure failures:
- **Retries with Exponential Backoff**: Transient downstream calls (such as external bank routing validation or settlement gateways) MUST implement bounded retries (max 3 attempts) with exponential backoff (e.g., 1s, 2s, 4s) so struggling downstream systems have time to recover.
- **Circuit Breaker & Safe Fallback**: If a downstream service fails repeatedly, the circuit breaker MUST trip open and return a safe, defined fallback state (`PENDING_SETTLEMENT_RETRY`) rather than crashing the service or losing the customer's transfer request.
- **Centralized Error Handling & Structured JSON Logging**: All exceptions and asynchronous errors MUST be handled centrally and logged using a standardized JSON structure:
  `{ "timestamp": "ISOString", "source": "string", "transactionId": "string", "error": "string", "context": {} }`.

## Multi-Agent Modernization Pipeline Governance
Rather than converting legacy mainframe code in a single unconstrained prompt, all mainframe-to-Java migrations in this repository MUST flow through 5 specialized agent skills with human-in-the-loop checkpoints:
1. **`1-code-dependency-analyzer`**: Parses legacy mainframe COBOL/JCL packages (`legacy-mainframe/`) and maps inputs, outputs, copybooks, and downstream dependencies.
2. **`2-requirements-extractor` (`/speckit-specify`)**: Extracts hidden mainframe business rules into `.specify` feature specifications (`specs/001-move-money-transfer/spec.md`).
3. **`3-architecture-planner` (`/speckit-plan`)**: Designs the target Spring Boot, Database, Resiliency, and Kafka blueprint (`specs/001-move-money-transfer/plan.md`) and validates compliance via a mandatory **Constitution Check**.
4. **`4-task-orchestrator` (`/speckit-tasks`)**: Breaks the blueprint into sequential, testable tasks (`specs/001-move-money-transfer/tasks.md`).
5. **`5-java-converter-validator` (`/speckit-implement`)**: Generates the Java Spring Boot code and JUnit parity tests verifying 100% functional equivalence with the legacy mainframe program.

## Decoupled Architecture and Storage Constraints
The application is designed using a decoupled, interface-driven service architecture (`TransferService`, `TransferRepository`, `TransferEventPublisher`, `ErrorHandlingService`) so it can run standalone with an in-memory repository during fast unit/parity testing and seamlessly bind to enterprise SQL Server / PostgreSQL and Apache Kafka in Pivotal Cloud Foundry (PCF) production environments.

## Testing and Quality Gates
Every converted business rule, fee calculation, idempotency check, and resilience fallback MUST be accompanied by automated unit tests (`JUnit 5`). Code formatting, compilation checks, and 100% legacy input/output parity tests MUST pass prior to merging via GitHub Actions CI/CD.

## Governance
This constitution is the supreme authority for engineering and AI-agent practices in the `agentic-payment-modernization` repository. All AI-generated specifications, architectural plans, code conversions, pull requests, and peer reviews MUST verify compliance with Principles I through VIII. Amendments to this constitution must be documented in the Sync Impact Report, increment the semantic version, and be ratified by the human maintainer.

**Version**: 1.1.0 | **Ratified**: 2026-10-27 | **Last Amended**: 2026-10-27
