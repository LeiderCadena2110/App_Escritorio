package co.edu.ucc.stockflow.assistant.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.assistant.domain.RestockPolicy;
import co.edu.ucc.stockflow.assistant.domain.RestockSuggestion;
import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestockAdvisorTest {

    private String sentPrompt;
    private boolean configured = true;

    private final AiModel fake = new AiModel() {
        public boolean isConfigured() { return configured; }
        public String description() { return "Falso"; }
        public String generateJson(String prompt, List<Attachment> files, String schema) { return "{}"; }
        public String generateText(String prompt) {
            sentPrompt = prompt;
            return "• Pedir 64 tenis a Deportes Pasto.";
        }
    };

    private static RestockSuggestion suggestion() {
        Product p = new Product();
        p.setSku("DEP-001");
        p.setName("Tenis running");
        p.setSupplierName("Deportes Pasto");
        p.setCostPrice(new BigDecimal("145000"));
        p.setStock(10);
        p.setMinStock(4);
        return RestockPolicy.standard().evaluate(p, 60).orElseThrow(); // buy 64
    }

    @Test
    void theModelGetsTheComputedNumbersAndTheRuleNotToChangeThem() {
        String answer = new RestockAdvisor(fake).summarize(List.of(suggestion()));

        assertEquals("• Pedir 64 tenis a Deportes Pasto.", answer);
        assertTrue(sentPrompt.contains("Urgente | DEP-001 Tenis running | Deportes Pasto | 10 | 4 | 60 | 5 días | 64 UND"));
        assertTrue(sentPrompt.contains("COSTO ESTIMADO TOTAL: $ 9.280.000"));
        assertTrue(sentPrompt.contains("no los cambies"));
    }

    @Test
    void estimatedCostIsUnitsTimesCurrentCost() {
        assertEquals(0, new BigDecimal("9280000").compareTo(RestockAdvisor.estimatedCost(List.of(suggestion()))));
    }

    @Test
    void withoutKeyItSaysHowToConfigureIt() {
        configured = false;
        assertEquals(ErrorCode.AI_NOT_CONFIGURED, assertThrows(BusinessException.class,
                () -> new RestockAdvisor(fake).summarize(List.of(suggestion()))).getCode());
    }
}
