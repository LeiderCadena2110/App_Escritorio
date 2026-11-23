package co.edu.ucc.stockflow.assistant.application;

import co.edu.ucc.stockflow.assistant.domain.RestockSuggestion;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import co.edu.ucc.stockflow.shared.util.Money;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * AI use case 2: turns the restock plan into a short buying note, grouped by supplier.
 *
 * <p>The numbers are computed by {@link co.edu.ucc.stockflow.assistant.domain.RestockPolicy} and
 * the estimated cost here; the model only writes them in plain Spanish and is told not to change
 * them. Only product names, suppliers and quantities are sent: no customer data.</p>
 */
@Service
public class RestockAdvisor {

    static final String INSTRUCTIONS = """
            Eres el asistente de compras de una tienda pequeña en Colombia. Abajo está el plan de \
            reabastecimiento que ya calculó el sistema. Escribe una nota corta para el dueño:
            - Empieza por los productos urgentes.
            - Agrupa el pedido por proveedor (los que no tienen proveedor van juntos al final).
            - Usa viñetas, máximo 8, y frases cortas.
            - Usa EXACTAMENTE las cantidades y valores del plan: no los cambies, no calcules otros \
            y no agregues productos.
            - Termina con el costo estimado total que aparece en el plan.
            No uses tablas ni formato Markdown de títulos.
            """;

    private final AiModel ai;

    public RestockAdvisor(AiModel ai) {
        this.ai = ai;
    }

    public boolean isAvailable() {
        return ai.isConfigured();
    }

    public String summarize(List<RestockSuggestion> plan) {
        if (!ai.isConfigured()) throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        return ai.generateText(INSTRUCTIONS + "\n" + describe(plan));
    }

    /** Cost of buying the suggested units at the current cost of each product. */
    public static BigDecimal estimatedCost(List<RestockSuggestion> plan) {
        return plan.stream()
                .map(s -> s.product().getCostPrice().multiply(BigDecimal.valueOf(s.suggestedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** The plan as plain lines the model can read (one product per line). */
    static String describe(List<RestockSuggestion> plan) {
        StringBuilder text = new StringBuilder("PLAN (prioridad | producto | proveedor | stock | mínimo | "
                + "vendidas en el periodo | días que alcanza | comprar | costo unitario)\n");
        for (RestockSuggestion s : plan) {
            var p = s.product();
            text.append(s.priority().label()).append(" | ")
                    .append(p.getSku()).append(" ").append(p.getName()).append(" | ")
                    .append(p.getSupplierName() == null || p.getSupplierName().isBlank() ? "sin proveedor" : p.getSupplierName())
                    .append(" | ").append(p.getStock())
                    .append(" | ").append(p.getMinStock())
                    .append(" | ").append(s.unitsSold())
                    .append(" | ").append(s.daysLeft() == null ? "sin ventas" : s.daysLeft() + " días")
                    .append(" | ").append(s.suggestedQuantity()).append(" ").append(p.getUnit())
                    .append(" | ").append(Money.format(p.getCostPrice()))
                    .append('\n');
        }
        text.append("COSTO ESTIMADO TOTAL: ").append(Money.format(estimatedCost(plan)));
        return text.toString();
    }
}
