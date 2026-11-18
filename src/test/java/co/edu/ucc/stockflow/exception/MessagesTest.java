package co.edu.ucc.stockflow.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessagesTest {

    @Test
    void everyErrorCodeHasAMessage() {
        for (ErrorCode code : ErrorCode.values()) {
            assertTrue(Messages.hasMessage(code), "Falta el mensaje error." + code.name());
        }
    }

    @Test
    void argumentsAreInsertedWithoutThousandsSeparators() {
        assertEquals("Solo hay 1200 PAR de Tenis.",
                new BusinessException(ErrorCode.STOCK_ONLY_AVAILABLE, 1200, "PAR", "Tenis").getMessage());
        assertEquals("No se encontró el producto con id 12345.",
                new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, 12345L).getMessage());
    }

    @Test
    void quotesAndAccentsAreKept() {
        assertEquals("Ya existe una categoría llamada \"Deportivos\".",
                new BusinessException(ErrorCode.CATEGORY_DUPLICATED, "Deportivos").getMessage());
    }

    @Test
    void insufficientStockKeepsItsDataAndCode() {
        InsufficientStockException e = new InsufficientStockException("DEP-001", "Tenis", 2, 5);
        assertEquals(ErrorCode.STOCK_INSUFFICIENT, e.getCode());
        assertEquals("No hay stock suficiente de DEP-001 · Tenis: hay 2 y se necesitan 5.", e.getMessage());
    }
}
