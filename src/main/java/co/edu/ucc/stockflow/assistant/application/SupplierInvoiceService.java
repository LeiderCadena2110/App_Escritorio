package co.edu.ucc.stockflow.assistant.application;

import co.edu.ucc.stockflow.assistant.domain.ProductMatcher;
import co.edu.ucc.stockflow.assistant.domain.PurchaseDraft;
import co.edu.ucc.stockflow.assistant.domain.SupplierInvoice;
import co.edu.ucc.stockflow.catalog.application.ProductService;
import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.inventory.application.StockService;
import co.edu.ucc.stockflow.inventory.domain.MovementType;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import co.edu.ucc.stockflow.shared.util.Money;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI use case 1: from a photo or PDF of a supplier's invoice to a purchase ready to review.
 *
 * <ol>
 *   <li>{@link #read(Path)} sends the file to the model with a JSON Schema, so the answer always
 *       has the same shape, and matches each line with a product of the catalog;</li>
 *   <li>the user reviews and corrects the draft on screen;</li>
 *   <li>{@link #register(PurchaseDraft)} saves the stock entries, all or none.</li>
 * </ol>
 */
@Service
public class SupplierInvoiceService {

    /** Shape of the answer (structured output): the model cannot answer anything else. */
    static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "supplierName": {"type": "string", "description": "Razón social o nombre del proveedor que emite la factura"},
                "supplierTaxId": {"type": "string", "description": "NIT del proveedor"},
                "invoiceNumber": {"type": "string", "description": "Número de la factura, con su prefijo"},
                "issueDate": {"type": "string", "description": "Fecha de emisión en formato AAAA-MM-DD"},
                "lines": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "code": {"type": "string", "description": "Código, referencia o código de barras del producto, si aparece"},
                      "description": {"type": "string", "description": "Descripción del producto tal como aparece"},
                      "quantity": {"type": "integer", "description": "Unidades compradas"},
                      "unitCost": {"type": "number", "description": "Valor unitario antes de IVA, en pesos"}
                    },
                    "required": ["description", "quantity", "unitCost"]
                  }
                },
                "total": {"type": "number", "description": "Total a pagar de la factura"}
              },
              "required": ["supplierName", "invoiceNumber", "lines"]
            }
            """;

    static final String PROMPT = """
            Eres el asistente de una tienda en Colombia. Lee la factura de compra de un proveedor que \
            está en el archivo adjunto y extrae sus datos con el esquema indicado.
            Reglas:
            - Una línea por cada producto. Ignora fletes, descuentos globales, subtotales, impuestos y retenciones.
            - unitCost es el valor de UNA unidad ANTES de IVA. Si la línea solo muestra el valor total, \
            divídelo por la cantidad. Si los precios incluyen IVA, quítale el IVA de esa línea.
            - Escribe los valores como números sin separador de miles (1.234.567,50 es 1234567.5).
            - Copia los textos tal como aparecen; no inventes productos ni datos. Si algo no se puede \
            leer, deja el texto vacío.
            """;

    private final AiModel ai;
    private final ProductService products;
    private final StockService stock;
    private final ObjectMapper json = new ObjectMapper();

    public SupplierInvoiceService(AiModel ai, ProductService products, StockService stock) {
        this.ai = ai;
        this.products = products;
        this.stock = stock;
    }

    public boolean isAvailable() {
        return ai.isConfigured();
    }

    /** Reads the invoice with the AI and prepares the purchase (nothing is saved yet). */
    public PurchaseDraft read(Path file) {
        return read(Attachment.read(file));
    }

    public PurchaseDraft read(Attachment file) {
        if (!ai.isConfigured()) throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        SupplierInvoice invoice = parse(ai.generateJson(PROMPT, List.of(file), SCHEMA));
        if (invoice.lines().isEmpty()) throw new BusinessException(ErrorCode.AI_INVOICE_WITHOUT_LINES);
        List<Product> catalog = products.findActive();
        List<PurchaseDraft.Line> lines = invoice.lines().stream()
                .map(l -> new PurchaseDraft.Line(l, ProductMatcher.find(l, catalog)))
                .toList();
        return new PurchaseDraft(invoice, lines);
    }

    /** Saves one purchase entry per included line. Returns how many were saved. */
    @Transactional
    public int register(PurchaseDraft draft) {
        List<PurchaseDraft.Line> lines = draft.included();
        if (lines.isEmpty()) throw new BusinessException(ErrorCode.AI_PURCHASE_EMPTY);
        SupplierInvoice invoice = draft.invoice();
        String reference = invoice.number().isBlank() ? "Factura de proveedor" : invoice.number();
        String notes = "Factura de " + (invoice.supplierName().isBlank() ? "proveedor" : invoice.supplierName())
                + " leída con IA";
        for (PurchaseDraft.Line line : lines) {
            stock.register(line.getProduct().getId(), MovementType.PURCHASE, line.getQuantity(),
                    line.getUnitCost(), reference, notes);
        }
        return lines.size();
    }

    /** Turns the model's JSON into the invoice, tolerating numbers written as text ("12.500"). */
    SupplierInvoice parse(String answer) {
        JsonNode root;
        try {
            root = json.readTree(answer);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_BAD_RESPONSE);
        }
        if (root == null || !root.isObject()) throw new BusinessException(ErrorCode.AI_BAD_RESPONSE);
        List<SupplierInvoice.Line> lines = new ArrayList<>();
        for (JsonNode l : root.path("lines")) {
            String description = text(l, "description");
            int quantity = number(l.path("quantity")).intValue();
            if (description.isEmpty() || quantity <= 0) continue;
            lines.add(new SupplierInvoice.Line(text(l, "code"), description, quantity,
                    Money.round(number(l.path("unitCost")).max(BigDecimal.ZERO))));
        }
        BigDecimal total = root.hasNonNull("total") ? Money.round(number(root.path("total"))) : null;
        return new SupplierInvoice(text(root, "supplierName"), text(root, "supplierTaxId"),
                text(root, "invoiceNumber"), text(root, "issueDate"), lines, total);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static BigDecimal number(JsonNode value) {
        if (value.isNumber()) return value.decimalValue();
        return Money.parseOr(value.asText(""), BigDecimal.ZERO);
    }
}
