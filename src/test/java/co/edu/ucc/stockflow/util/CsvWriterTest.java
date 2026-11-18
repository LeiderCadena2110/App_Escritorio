package co.edu.ucc.stockflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CsvWriterTest {

    @Test
    void quotesValuesWithSeparatorsOrQuotes() {
        assertEquals("simple", CsvWriter.escape("simple"));
        assertEquals("\"a;b\"", CsvWriter.escape("a;b"));
        assertEquals("\"dice \"\"hola\"\"\"", CsvWriter.escape("dice \"hola\""));
    }

    @Test
    void decimalsUseComma() {
        assertEquals("1234,50", CsvWriter.toText(new BigDecimal("1234.50")));
        assertEquals("", CsvWriter.toText(null));
    }
}
