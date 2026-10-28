package com.movemoney.transfer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransferType Parity & Lookup Tests")
class TransferTypeTest {

    @ParameterizedTest(name = "Cobol code {0} maps to TransferType.{1}")
    @CsvSource({
            "INT-BRKBNK, INT_BRKBNK",
            "INT-RETBRK, INT_RETBRK",
            "ACH, ACH",
            "WIRE, WIRE"
    })
    void shouldMapCobolCodesCorrectly(String cobolCode, TransferType expectedType) {
        assertThat(TransferType.fromString(cobolCode)).isEqualTo(expectedType);
        assertThat(expectedType.getCobolCode()).isEqualTo(cobolCode);
        assertThat(expectedType.getDescription()).isNotBlank();
    }

    @Test
    void shouldMapByEnumName() {
        assertThat(TransferType.fromString("INT_BRKBNK")).isEqualTo(TransferType.INT_BRKBNK);
        assertThat(TransferType.fromString("int_retbrk")).isEqualTo(TransferType.INT_RETBRK);
    }

    @Test
    void shouldRejectInvalidTransferType() {
        assertThatThrownBy(() -> TransferType.fromString("INVALID_TYPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown transfer type");
    }

    @Test
    void shouldRejectNullOrBlank() {
        assertThatThrownBy(() -> TransferType.fromString(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TransferType.fromString("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

