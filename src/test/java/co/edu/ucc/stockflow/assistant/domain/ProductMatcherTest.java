package co.edu.ucc.stockflow.assistant.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import co.edu.ucc.stockflow.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductMatcherTest {

    private static Product product(String sku, String barcode, String name) {
        Product p = new Product();
        p.setSku(sku);
        p.setBarcode(barcode);
        p.setName(name);
        return p;
    }

    private final Product running = product("DEP-001", "7709000000012", "Tenis running Ultra 3");
    private final Product socks = product("ACC-001", null, "Medias deportivas x3");
    private final Product guayos = product("DEP-002", null, "Guayos fútbol Pro FG");
    private final List<Product> catalog = List.of(running, socks, guayos);

    private static SupplierInvoice.Line line(String code, String description) {
        return new SupplierInvoice.Line(code, description, 1, BigDecimal.TEN);
    }

    @Test
    void theCodeWinsWhenItIsTheSkuOrTheBarcode() {
        ProductMatcher.Match bySku = ProductMatcher.find(line("acc-001", "lo que sea"), catalog);
        assertSame(socks, bySku.product());
        assertEquals(MatchQuality.CODE, bySku.quality());
        assertSame(running, ProductMatcher.find(line("7709000000012", "x"), catalog).product());
    }

    @Test
    void otherwiseTheNameIsFoundInsideALongerDescription() {
        ProductMatcher.Match m = ProductMatcher.find(line("REF-88", "TENIS RUNNING ULTRA 3 NEGRO T40"), catalog);
        assertSame(running, m.product());
        assertEquals(MatchQuality.NAME, m.quality());
        assertSame(guayos, ProductMatcher.find(line("", "Guayos futbol pro FG blanco"), catalog).product());
    }

    @Test
    void aDescriptionWithFewWordsOfAnyProductHasNoMatch() {
        ProductMatcher.Match m = ProductMatcher.find(line("", "Sandalia playera dama"), catalog);
        assertNull(m.product());
        assertEquals(MatchQuality.NONE, m.quality());
    }

    @Test
    void wordsIgnoreAccentsCaseAndConnectors() {
        assertEquals(java.util.Set.of("guayos", "futbol", "pro"), ProductMatcher.words("Guayos de Fútbol PRO x"));
        assertEquals(java.util.Set.of("medias", "deportivas", "3"), ProductMatcher.words("Medias deportivas x 3"));
    }
}
