package co.edu.ucc.stockflow.assistant.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.assistant.domain.MatchQuality;
import co.edu.ucc.stockflow.assistant.domain.PurchaseDraft;
import co.edu.ucc.stockflow.catalog.application.ProductService;
import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.inventory.application.StockService;
import co.edu.ucc.stockflow.inventory.domain.MovementType;
import co.edu.ucc.stockflow.inventory.domain.StockMovement;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/** The whole use case with a fake model that "reads" a fixed invoice: no internet, no key. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stockflow-ai-test;DB_CLOSE_DELAY=-1",
        "logging.file.name="
})
@Transactional
class SupplierInvoiceServiceTest {

    /** What the model answers, as Gemini would with the JSON Schema. Prices come as text on purpose. */
    static final String ANSWER = """
            {"supplierName": "Distribuidora Andina S.A.S.", "supplierTaxId": "900123456-7",
             "invoiceNumber": "FE-4521", "issueDate": "2026-11-20",
             "lines": [
               {"code": "AI-001", "description": "Tenis prueba azul", "quantity": 6, "unitCost": "12.000"},
               {"code": "", "description": "MEDIAS PRUEBA TOBILLERAS blancas", "quantity": 12, "unitCost": 3500},
               {"code": "", "description": "Sandalia playera dama", "quantity": 4, "unitCost": 20000},
               {"code": "", "description": "Flete", "quantity": 0, "unitCost": 15000}
             ],
             "total": 154700}
            """;

    @TestConfiguration
    static class FakeAi {
        @Bean
        @Primary
        AiModel fakeModel() {
            return new AiModel() {
                public boolean isConfigured() { return true; }
                public String description() { return "Falso"; }
                public String generateJson(String prompt, List<Attachment> files, String schema) { return ANSWER; }
                public String generateText(String prompt) { return "Resumen"; }
            };
        }
    }

    @Autowired
    private SupplierInvoiceService service;
    @Autowired
    private ProductService products;
    @Autowired
    private StockService stock;

    private Product product(String sku, String name, int units, String cost) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCostPrice(new BigDecimal(cost));
        p.setSalePrice(new BigDecimal("50000"));
        p.setStock(units);
        return products.create(p);
    }

    private PurchaseDraft read() {
        return service.read(new Attachment("image/jpeg", new byte[]{1}));
    }

    @Test
    void readsTheInvoiceAndMatchesTheProducts() {
        Product shoes = product("AI-001", "Tenis prueba", 2, "10000");
        Product socks = product("AI-002", "Medias prueba tobilleras", 0, "3000");

        PurchaseDraft draft = read();

        assertEquals("FE-4521", draft.invoice().number());
        assertEquals(3, draft.lines().size(), "the line with quantity 0 (freight) is dropped");
        assertEquals(shoes.getId(), draft.lines().get(0).getProduct().getId());
        assertEquals(MatchQuality.CODE, draft.lines().get(0).getQuality());
        assertEquals(new BigDecimal("12000.00"), draft.lines().get(0).getUnitCost());
        assertEquals(socks.getId(), draft.lines().get(1).getProduct().getId());
        assertEquals(MatchQuality.NAME, draft.lines().get(1).getQuality());
        assertNull(draft.lines().get(2).getProduct());
        assertFalse(draft.lines().get(2).willRegister(), "lines without product are not saved");
    }

    @Test
    void registeringAddsStockAtTheAverageCostWithTheInvoiceAsReference() {
        Product shoes = product("AI-001", "Tenis prueba", 2, "10000");
        product("AI-002", "Medias prueba tobilleras", 0, "3000");
        PurchaseDraft draft = read();
        draft.lines().get(1).setQuantity(10); // the user corrected it on screen

        assertEquals(2, service.register(draft));

        Product after = products.findById(shoes.getId()).orElseThrow();
        assertEquals(8, after.getStock());
        // (2 × 10.000 + 6 × 12.000) / 8 = 11.500
        assertEquals(0, new BigDecimal("11500").compareTo(after.getCostPrice()));
        StockMovement last = stock.history(shoes.getId()).get(0);
        assertEquals(MovementType.PURCHASE, last.getType());
        assertEquals("FE-4521", last.getReference());
        assertTrue(last.getNotes().contains("Distribuidora Andina"));
        assertEquals(10, products.findBySku("AI-002").orElseThrow().getStock());
    }

    @Test
    void nothingToRegisterIsAClearError() {
        PurchaseDraft draft = read(); // no product of the catalog looks like these lines
        assertEquals(ErrorCode.AI_PURCHASE_EMPTY,
                assertThrows(BusinessException.class, () -> service.register(draft)).getCode());
    }

    @Test
    void anAnswerThatIsNotJsonIsABadResponse() {
        assertEquals(ErrorCode.AI_BAD_RESPONSE,
                assertThrows(BusinessException.class, () -> service.parse("Lo siento, no puedo")).getCode());
    }
}
