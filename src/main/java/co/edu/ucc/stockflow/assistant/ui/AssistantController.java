package co.edu.ucc.stockflow.assistant.ui;

import co.edu.ucc.stockflow.assistant.application.AiModel;
import co.edu.ucc.stockflow.assistant.application.RestockAdvisor;
import co.edu.ucc.stockflow.assistant.application.RestockPlanner;
import co.edu.ucc.stockflow.assistant.application.SupplierInvoiceService;
import co.edu.ucc.stockflow.assistant.domain.PurchaseDraft;
import co.edu.ucc.stockflow.assistant.domain.RestockPolicy;
import co.edu.ucc.stockflow.assistant.domain.RestockPriority;
import co.edu.ucc.stockflow.assistant.domain.RestockSuggestion;
import co.edu.ucc.stockflow.catalog.application.ProductService;
import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.shared.ui.Icon;
import co.edu.ucc.stockflow.shared.ui.Navigator;
import co.edu.ucc.stockflow.shared.ui.SectionController;
import co.edu.ucc.stockflow.shared.ui.TaskRunner;
import co.edu.ucc.stockflow.shared.ui.Toasts;
import co.edu.ucc.stockflow.shared.ui.component.FileChoosers;
import co.edu.ucc.stockflow.shared.ui.component.Tables;
import co.edu.ucc.stockflow.shared.util.Money;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Asistente IA" section: reading supplier invoices into purchases and the restock plan with an
 * optional AI buying note. The plan works without a key; only the AI buttons need one.
 */
@Component
@Scope("prototype")
public class AssistantController implements SectionController {

    private static final List<Integer> PERIODS = List.of(15, 30, 60, 90);

    @FXML private Label aiStatus;
    @FXML private Button configureButton;
    @FXML private Button readInvoiceButton;
    @FXML private Label planSummary;
    @FXML private ComboBox<Integer> periodBox;
    @FXML private Button summaryButton;
    @FXML private TableView<RestockSuggestion> planTable;
    @FXML private VBox notePane;
    @FXML private Label noteSource;
    @FXML private TextArea noteArea;

    private final AiModel ai;
    private final RestockPlanner planner;
    private final RestockAdvisor advisor;
    private final SupplierInvoiceService invoices;
    private final ProductService products;
    private final AiConfigDialog configDialog;
    private final SupplierInvoiceDialog invoiceDialog;
    private final FileChoosers files;
    private final Navigator navigator;
    private final TaskRunner tasks;
    private final Toasts toasts;

    private final ObservableList<RestockSuggestion> plan = FXCollections.observableArrayList();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanProperty configured = new SimpleBooleanProperty(false);

    public AssistantController(AiModel ai, RestockPlanner planner, RestockAdvisor advisor,
                               SupplierInvoiceService invoices, ProductService products, AiConfigDialog configDialog,
                               SupplierInvoiceDialog invoiceDialog, FileChoosers files, Navigator navigator,
                               TaskRunner tasks, Toasts toasts) {
        this.ai = ai;
        this.planner = planner;
        this.advisor = advisor;
        this.invoices = invoices;
        this.products = products;
        this.configDialog = configDialog;
        this.invoiceDialog = invoiceDialog;
        this.files = files;
        this.navigator = navigator;
        this.tasks = tasks;
        this.toasts = toasts;
    }

    @FXML
    private void initialize() {
        readInvoiceButton.setGraphic(Icon.DOCUMENT.node(16));
        summaryButton.setGraphic(Icon.SPARKLES.node(16));
        configureButton.setGraphic(Icon.SETTINGS.node(14));
        readInvoiceButton.disableProperty().bind(busy);
        summaryButton.disableProperty().bind(busy.or(Bindings.isEmpty(plan)));

        periodBox.getItems().setAll(PERIODS);
        periodBox.setValue(30);
        periodBox.setConverter(new StringConverter<>() {
            public String toString(Integer days) { return days == null ? "" : days + " días"; }
            public Integer fromString(String text) { return 30; }
        });
        periodBox.valueProperty().addListener((o, a, b) -> loadPlan());
        buildTable();
    }

    private void buildTable() {
        planTable.setItems(plan);
        planTable.getColumns().setAll(List.of(
                Tables.badge("Prioridad", s -> s.priority().label(),
                        s -> s.priority() == RestockPriority.URGENT ? "badge-danger" : "badge-warning", 95),
                Tables.text("Código", s -> s.product().getSku(), 90),
                Tables.text("Producto", s -> s.product().getName(), 220),
                Tables.text("Proveedor", s -> orDash(s.product().getSupplierName()), 150),
                Tables.integer("Stock", s -> s.product().getStock(), 65),
                Tables.integer("Mín.", s -> s.product().getMinStock(), 55),
                Tables.integer("Vendidas", RestockSuggestion::unitsSold, 75),
                Tables.text("Alcanza", s -> s.daysLeft() == null ? "Sin ventas" : s.daysLeft() + " días", 85),
                Tables.integer("Comprar", RestockSuggestion::suggestedQuantity, 80)));
        Tables.fill(planTable, 2);
        Tables.placeholder(planTable, "Todo en orden: ningún producto necesita compra por ahora.");
        Tables.onOpen(planTable, s -> navigator.open(co.edu.ucc.stockflow.shared.ui.Section.INVENTORY, s.product()));
    }

    @Override
    public void onShow() {
        showStatus();
        loadPlan();
    }

    private void showStatus() {
        configured.set(ai.isConfigured());
        aiStatus.getStyleClass().removeAll("badge-ok", "badge-warning");
        if (configured.get()) {
            aiStatus.setText(ai.description());
            aiStatus.getStyleClass().add("badge-ok");
        } else {
            aiStatus.setText("IA sin configurar");
            aiStatus.getStyleClass().add("badge-warning");
        }
    }

    private void loadPlan() {
        RestockPolicy policy = policy();
        tasks.run("No se pudo calcular el plan de compras", () -> planner.plan(policy), result -> {
            plan.setAll(result);
            long urgent = result.stream().filter(s -> s.priority() == RestockPriority.URGENT).count();
            planSummary.setText(result.isEmpty() ? "Calculado con el stock, el mínimo y las ventas de cada producto."
                    : result.size() + " productos · " + urgent + (urgent == 1 ? " urgente" : " urgentes") + " · costo estimado "
                    + Money.format(RestockAdvisor.estimatedCost(result)) + " (al costo actual)");
        });
    }

    private RestockPolicy policy() {
        RestockPolicy standard = RestockPolicy.standard();
        Integer days = periodBox.getValue();
        return new RestockPolicy(days == null ? standard.periodDays() : days, standard.leadTimeDays(),
                standard.coverageDays());
    }

    @FXML
    private void configure() {
        configDialog.show(navigator.window()).ifPresent(description -> {
            showStatus();
            toasts.show(Toasts.Kind.SUCCESS, "Inteligencia artificial lista", description + " respondió correctamente.");
        });
        showStatus(); // the key may have been saved even if the test failed
    }

    /** Needs a key: if there is none, opens the configuration first. */
    private boolean ensureConfigured() {
        if (ai.isConfigured()) return true;
        configure();
        return ai.isConfigured();
    }

    @FXML
    private void readInvoice() {
        if (!ensureConfigured()) return;
        Path file = files.openImageOrPdf(navigator.window(), "Factura del proveedor");
        if (file == null) return;
        busy.set(true);
        toasts.show(Toasts.Kind.INFO, "Leyendo la factura…", "Puede tardar unos segundos.");
        record Read(PurchaseDraft draft, List<Product> catalog) {
        }
        tasks.run("No se pudo leer la factura", () -> new Read(invoices.read(file), products.findActive()), read -> {
            busy.set(false);
            invoiceDialog.show(navigator.window(), read.draft(), read.catalog()).ifPresent(count -> {
                toasts.show(Toasts.Kind.SUCCESS, "Compra registrada", count + " productos sumados al inventario "
                        + "con la referencia " + orDash(read.draft().invoice().number()) + ".");
                loadPlan();
            });
        }, () -> busy.set(false));
    }

    @FXML
    private void summarize() {
        if (plan.isEmpty() || !ensureConfigured()) return;
        List<RestockSuggestion> current = List.copyOf(plan);
        busy.set(true);
        tasks.run("No se pudo escribir la nota de compra", () -> advisor.summarize(current), note -> {
            busy.set(false);
            noteArea.setText(note);
            noteSource.setText("Escrita por " + ai.description() + " con las cantidades de la tabla. Revísela antes de usarla.");
            notePane.setManaged(true);
            notePane.setVisible(true);
        }, () -> busy.set(false));
    }

    @FXML
    private void copyNote() {
        ClipboardContent content = new ClipboardContent();
        content.putString(noteArea.getText());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        toasts.show(Toasts.Kind.SUCCESS, "Nota copiada", "Péguela en WhatsApp o en un correo al proveedor.");
    }

    private static String orDash(String text) {
        return text == null || text.isBlank() ? "—" : text;
    }
}
