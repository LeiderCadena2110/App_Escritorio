package co.edu.ucc.stockflow.settings.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SequenceServiceTest {

    @Test
    void invoiceNumbersHaveSixDigits() {
        assertEquals("FV-000042", SequenceService.format("FV-", 42));
        assertEquals("1234567", SequenceService.format(null, 1234567));
    }
}
