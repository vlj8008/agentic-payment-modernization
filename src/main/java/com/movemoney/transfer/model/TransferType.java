package com.movemoney.transfer.model;

import java.util.Arrays;

/**
 * Payment rail transfer types supported by the Move-Money modern payment service.
 * <p>
 * Maps directly to legacy mainframe {@code XFERPROC.cbl} {@code WS-XFER-TYPE} values:
 * <ul>
 *   <li>{@code INT-BRKBNK} &mdash; Internal Brokerage-to-Bank Transfer</li>
 *   <li>{@code INT-RETBRK} &mdash; Internal Retirement-to-Brokerage Transfer</li>
 *   <li>{@code ACH} &mdash; Automated Clearing House Transfer</li>
 *   <li>{@code WIRE} &mdash; Domestic/International Wire Transfer</li>
 * </ul>
 */
public enum TransferType {

    /**
     * Internal Brokerage-to-Bank Transfer ($0.00 fee).
     */
    INT_BRKBNK("INT-BRKBNK", "Internal Brokerage-to-Bank Transfer"),

    /**
     * Internal Retirement-to-Brokerage Transfer ($0.00 fee).
     */
    INT_RETBRK("INT-RETBRK", "Internal Retirement-to-Brokerage Transfer"),

    /**
     * Automated Clearing House Transfer ($0.00 fee).
     */
    ACH("ACH", "Automated Clearing House Transfer"),

    /**
     * Domestic or International Wire Transfer ($25.00 fee under $10,000; waived for >= $10,000).
     */
    WIRE("WIRE", "Wire Transfer");

    private final String cobolCode;
    private final String description;

    TransferType(String cobolCode, String description) {
        this.cobolCode = cobolCode;
        this.description = description;
    }

    public String getCobolCode() {
        return cobolCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parses a string representation of transfer type, matching either the enum name or the legacy COBOL code.
     *
     * @param value the raw string value (e.g., "INT-BRKBNK", "INT_BRKBNK", "WIRE")
     * @return the matching TransferType
     * @throws IllegalArgumentException if no match is found
     */
    public static TransferType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transfer type must not be null or blank");
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(t -> t.name().equals(normalized) || t.cobolCode.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown transfer type: " + value));
    }
}

