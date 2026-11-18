package co.edu.ucc.stockflow.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.catalog.domain.InsufficientStockException;
import co.edu.ucc.stockflow.catalog.domain.Product;
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
        assertEquals("Solo hay 1200 PAR de DEP-001 · Tenis y se necesitan 1500.",
                new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "DEP-001", "Tenis", 1200, 1500, "PAR").getMessage());
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
        Product shoes = new Product();
        shoes.setSku("DEP-001");
        shoes.setName("Tenis");
        shoes.setUnit("PAR");
        shoes.setStock(2);
        InsufficientStockException e = assertThrows(InsufficientStockException.class, () -> shoes.checkCanTakeOut(5));
        assertEquals(ErrorCode.STOCK_INSUFFICIENT, e.getCode());
        assertEquals(2, e.getAvailable());
        assertEquals("Solo hay 2 PAR de DEP-001 · Tenis y se necesitan 5.", e.getMessage());

        shoes.setStock(0);
        assertEquals(ErrorCode.STOCK_SOLD_OUT, assertThrows(InsufficientStockException.class,
                () -> shoes.checkCanTakeOut(1)).getCode());
        shoes.setStock(5);
        shoes.checkCanTakeOut(5); // exactly the stock is allowed
    }
}
