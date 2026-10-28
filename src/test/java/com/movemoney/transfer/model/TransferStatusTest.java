package com.movemoney.transfer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransferStatus Parity & Lookup Tests")
class TransferStatusTest {

    @ParameterizedTest(name = "Status code {0} maps to TransferStatus.{1} with HTTP {2}")
    @CsvSource({
            "00, COMPLETED, 200",
            "10, REJECTED_INSUFFICIENT_FUNDS, 422",
            "20, FLAGGED_FOR_FRAUD_REVIEW, 202",
            "30, REJECTED_ACCOUNT_INACTIVE, 422",
            "90, DUPLICATE_IGNORED, 200",
            "PENDING_SETTLEMENT_RETRY, PENDING_SETTLEMENT_RETRY, 202"
    })
    void shouldMapLegacyAndResiliencyStatusCodesCorrectly(String code, TransferStatus expectedStatus, int expectedHttp) {
        TransferStatus resolved = TransferStatus.fromCode(code);
        assertThat(resolved).isEqualTo(expectedStatus);
        assertThat(resolved.getCode()).isEqualTo(code);
        assertThat(resolved.getHttpStatusCode()).isEqualTo(expectedHttp);
        assertThat(resolved.getMessage()).isNotBlank();
    }

    @Test
    void shouldLookupByName() {
        assertThat(TransferStatus.fromCode("COMPLETED")).isEqualTo(TransferStatus.COMPLETED);
        assertThat(TransferStatus.fromCode("rejected_insufficient_funds")).isEqualTo(TransferStatus.REJECTED_INSUFFICIENT_FUNDS);
    }

    @Test
    void shouldRejectInvalidStatusCode() {
        assertThatThrownBy(() -> TransferStatus.fromCode("99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown transfer status code");
    }

    @Test
    void shouldRejectNullOrBlank() {
        assertThatThrownBy(() -> TransferStatus.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TransferStatus.fromCode("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

