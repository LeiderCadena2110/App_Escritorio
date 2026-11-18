package co.edu.ucc.stockflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DataSeederTest {

    @Test
    void sampleBarcodesHaveAValidEan13CheckDigit() {
        assertEquals("4006381333931", DataSeeder.ean13("400638133393"));
        assertEquals("7709000000012", DataSeeder.ean13("770900000001"));
    }
}
