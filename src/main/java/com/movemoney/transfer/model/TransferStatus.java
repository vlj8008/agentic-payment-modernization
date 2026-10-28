package com.movemoney.transfer.model;

import java.util.Arrays;

/**
 * Payment transfer status codes and business outcomes.
 * <p>
 * Maintains 100% parity with legacy mainframe {@code XFERPROC.cbl} status codes:
 * <ul>
 *   <li>{@code 00} &mdash; COMPLETED: Transfer processed successfully</li>
 *   <li>{@code 10} &mdash; REJECTED: Insufficient available balance</li>
 *   <li>{@code 20} &mdash; FLAGGED: Exceeds $50,000 limit - manual compliance review</li>
 *   <li>{@code 30} &mdash; REJECTED: Source or target account inactive (soft deleted)</li>
 *   <li>{@code 90} &mdash; DUPLICATE: Idempotency key already processed (duplicate ignored)</li>
 * </ul>
 * Also defines the modern cloud resiliency safe fallback state:
 * <ul>
 *   <li>{@code PENDING_SETTLEMENT_RETRY} &mdash; Downstream settlement transient failure fallback</li>
 * </ul>
 */
public enum TransferStatus {

    /**
     * Legacy Status Code 00: Completed transfer successfully.
     */
    COMPLETED("00", "COMPLETED: TRANSFER PROCESSED SUCCESSFULLY", 200),

    /**
     * Legacy Status Code 10: Rejected due to insufficient available funds.
     */
    REJECTED_INSUFFICIENT_FUNDS("10", "REJECTED: INSUFFICIENT AVAILABLE BALANCE", 422),

    /**
     * Legacy Status Code 20: Flagged for high-value fraud and compliance review (> $50,000.00).
     */
    FLAGGED_FOR_FRAUD_REVIEW("20", "FLAGGED: EXCEEDS $50,000 LIMIT - MANUAL REVIEW", 202),

    /**
     * Legacy Status Code 30: Rejected because the source or target account is inactive.
     */
    REJECTED_ACCOUNT_INACTIVE("30", "REJECTED: SOURCE OR TARGET ACCOUNT INACTIVE", 422),

    /**
     * Legacy Status Code 90: Duplicate idempotency key; returns cached result without re-debiting.
     */
    DUPLICATE_IGNORED("90", "DUPLICATE: IDEMPOTENCY KEY ALREADY PROCESSED", 200),

    /**
     * Cloud Resiliency Safe Fallback: Downstream settlement gateway transient failure.
     */
    PENDING_SETTLEMENT_RETRY("PENDING_SETTLEMENT_RETRY", "PENDING: DOWNSTREAM SETTLEMENT RETRY REQUIRED", 202);

    private final String code;
    private final String message;
    private final int httpStatusCode;

    TransferStatus(String code, String message, int httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Resolves a {@code TransferStatus} from a legacy 2-digit status code or resiliency code name.
     *
     * @param code the status code (e.g., "00", "10", "20", "30", "90", "PENDING_SETTLEMENT_RETRY")
     * @return matching TransferStatus
     * @throws IllegalArgumentException if no matching status is found
     */
    public static TransferStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Status code must not be null or blank");
        }
        String normalized = code.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(normalized) || s.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown transfer status code: " + code));
    }
}

