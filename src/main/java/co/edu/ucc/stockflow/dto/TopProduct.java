package co.edu.ucc.stockflow.dto;

import java.math.BigDecimal;

/** A product with the units sold and the money it brought in a period. */
public record TopProduct(String sku, String name, int quantity, BigDecimal revenue, BigDecimal profit) {
}
