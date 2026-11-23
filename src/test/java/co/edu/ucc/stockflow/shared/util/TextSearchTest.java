package co.edu.ucc.stockflow.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextSearchTest {

    @Test
    void normalizeRemovesAccentsAndCase() {
        assertEquals("pena jose", TextSearch.normalize("  Peña JOSÉ "));
        assertEquals("", TextSearch.normalize(null));
    }

    @Test
    void wordsCanComeInAnyOrderAndWithoutAccents() {
        assertTrue(TextSearch.matches("jose per", "Pérez, José"));
        assertTrue(TextSearch.matches("zapatilla nike", "Nike Zapatilla Running"));
        assertTrue(TextSearch.matches("BOGOTA", "Bogotá D.C."));
    }

    @Test
    void everyWordMustAppearInSomeField() {
        assertTrue(TextSearch.matches("maria 1085", "María Gómez", "1085123456"));
        assertFalse(TextSearch.matches("maria 9999", "María Gómez", "1085123456"));
    }

    @Test
    void emptyQueryMatchesEverythingAndNullFieldsAreIgnored() {
        assertTrue(TextSearch.matches("  ", "lo que sea"));
        assertTrue(TextSearch.matches("abc", null, "ABC"));
    }
}
