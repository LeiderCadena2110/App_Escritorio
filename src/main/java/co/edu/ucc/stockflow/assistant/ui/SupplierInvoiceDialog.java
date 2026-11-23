package co.edu.ucc.stockflow.assistant.ui;

import co.edu.ucc.stockflow.assistant.application.SupplierInvoiceService;
import co.edu.ucc.stockflow.assistant.domain.MatchQuality;
import co.edu.ucc.stockflow.assistant.domain.PurchaseDraft;
import co.edu.ucc.stockflow.assistant.domain.SupplierInvoice;
import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.shared.ui.Dialogs;
import co.edu.ucc.stockflow.shared.ui.TaskRunner;
import co.edu.ucc.stockflow.shared.ui.component.AutoCompleteField;
import co.edu.ucc.stockflow.shared.ui.component.FormGrid;
import co.edu.ucc.stockflow.shared.ui.dialog.FormDialog;
import co.edu.ucc.stockflow.shared.util.Money;
import co.edu.ucc.stockflow.shared.util.TextSearch;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Shows what the AI read from a supplier's invoice so the user checks it before anything is saved:
 * each line with the product it seems to be (changeable), quantity and unit cost (editable), and a
 * check to include it. Lines without a product are left out unless the user chooses one.
 */
@Component
public class SupplierInvoiceDialog {

    private final SupplierInvoiceService service;
    private final Dialogs dialogs;
    private final TaskRunner tasks;

    public SupplierInvoiceDialog(SupplierInvoiceService service, Dialogs dialogs, TaskRunner tasks) {
        this.service = service;
        this.dialogs = dialogs;
        this.tasks = tasks;
    }

    /** One row of the screen, tied to one line of the draft. */
    private record Row(PurchaseDraft.Line line, CheckBox include, AutoCompleteField<Product> product,
                       TextField quantity, TextField cost) {
    }

    /** Returns how many stock entries were registered, or empty if the user cancelled. */
    public Optional<Integer> show(Window owner, PurchaseDraft draft, List<Product> catalog) {
        FormDialog<Integer> fd = new FormDialog<>(dialogs, tasks, owner, "Revisar factura de proveedor",
                "Registrar compra");
        fd.dialog().getDialogPane().setPrefWidth(980);
        FormGrid form = fd.form();

        SupplierInvoice invoice = draft.invoice();
        Label header = new Label(orDash(invoice.supplierName())
                + (invoice.supplierTaxId().isBlank() ? "" : " · NIT " + invoice.supplierTaxId())
                + "   ·   Factura " + orDash(invoice.number())
                + "   ·   " + orDash(invoice.issueDate()));
        header.getStyleClass().add("card-title");
        form.addWide(header);
        Label help = new Label("Revise cada línea: el producto que encontró StockFlow, la cantidad y el costo "
                + "unitario sin IVA. Solo se registran las líneas marcadas.");
        help.getStyleClass().add("muted");
        help.setWrapText(true);
        form.addWide(help);

        ObservableList<Product> products = FXCollections.observableArrayList(catalog);
        GridPane grid = linesGrid();
        List<Row> rows = new ArrayList<>();
        Label totals = new Label();
        totals.getStyleClass().add("total-value");
        Runnable refresh = () -> totals.setText(summary(rows, invoice));

        int r = 1;
        for (PurchaseDraft.Line line : draft.lines()) {
            Row row = row(line, products);
            rows.add(row);
            addRow(grid, r++, row);
            row.include().selectedProperty().addListener((o, a, b) -> refresh.run());
            row.quantity().textProperty().addListener((o, a, b) -> refresh.run());
            row.cost().textProperty().addListener((o, a, b) -> refresh.run());
            row.product().valueProperty().addListener((o, old, chosen) -> {
                if (chosen != null) row.include().setSelected(true);
                refresh.run();
            });
        }
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(Math.min(30 + 46 * draft.lines().size(), 380));
        form.addWide(scroll);
        form.addWide(totals);
        refresh.run();

        return fd.show(() -> {
            for (Row row : rows) {
                row.line().setInclude(row.include().isSelected());
                row.line().setProduct(row.product().getValue());
                row.line().setQuantity(FormGrid.intValue(row.quantity()));
                row.line().setUnitCost(FormGrid.moneyValue(row.cost()));
            }
            return () -> service.register(draft);
        }, null);
    }

    private static GridPane linesGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.getColumnConstraints().addAll(new ColumnConstraints(28), grow(260), grow(260),
                new ColumnConstraints(70), new ColumnConstraints(110), new ColumnConstraints(120));
        String[] titles = {"", "En la factura", "Producto en StockFlow", "Cantidad", "Costo unit.", ""};
        for (int c = 0; c < titles.length; c++) {
            Label t = new Label(titles[c]);
            t.getStyleClass().add("form-label");
            grid.add(t, c, 0);
        }
        return grid;
    }

    private static ColumnConstraints grow(double min) {
        ColumnConstraints c = new ColumnConstraints(min, min, Double.MAX_VALUE);
        c.setHgrow(Priority.ALWAYS);
        return c;
    }

    private static Row row(PurchaseDraft.Line line, ObservableList<Product> products) {
        CheckBox include = new CheckBox();
        include.setSelected(line.isInclude());
        AutoCompleteField<Product> product = new AutoCompleteField<>(products,
                p -> p.getSku() + " · " + p.getName(),
                p -> "Stock " + p.getStock() + " · costo " + Money.format(p.getCostPrice()),
                (p, text) -> TextSearch.matches(text, p.getSku(), p.getName(), p.getBarcode()));
        product.setPromptText("Elegir producto…");
        product.setValue(line.getProduct());
        product.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = FormGrid.integer(line.getQuantity(), false);
        TextField cost = FormGrid.money(line.getUnitCost());
        return new Row(line, include, product, quantity, cost);
    }

    private static void addRow(GridPane grid, int r, Row row) {
        SupplierInvoice.Line source = row.line().getSource();
        Label description = new Label(source.description());
        description.setWrapText(true);
        Label detail = new Label((source.code().isBlank() ? "" : "Código " + source.code() + " · ")
                + source.quantity() + " × " + Money.format(source.unitCost()));
        detail.getStyleClass().add("muted");
        VBox read = new VBox(1, description, detail);

        MatchQuality quality = row.line().getQuality();
        Label badge = new Label(quality.label());
        badge.getStyleClass().addAll("badge", switch (quality) {
            case CODE -> "badge-ok";
            case NAME -> "badge-warning";
            case NONE -> "badge-danger";
        });

        grid.add(row.include(), 0, r);
        grid.add(read, 1, r);
        grid.add(row.product(), 2, r);
        grid.add(row.quantity(), 3, r);
        grid.add(row.cost(), 4, r);
        grid.add(badge, 5, r);
        GridPane.setHalignment(row.include(), javafx.geometry.HPos.CENTER);
        read.setAlignment(Pos.CENTER_LEFT);
    }

    /** "Se registrarán 3 de 4 líneas · $ 120.000 sin IVA · total de la factura $ 154.700". */
    private static String summary(List<Row> rows, SupplierInvoice invoice) {
        int count = 0;
        BigDecimal cost = BigDecimal.ZERO;
        for (Row row : rows) {
            if (!row.include().isSelected() || row.product().getValue() == null) continue;
            int units = Math.max(0, (int) Money.parseOr(row.quantity().getText(), BigDecimal.ZERO).longValue());
            if (units == 0) continue;
            count++;
            cost = cost.add(Money.parseOr(row.cost().getText(), BigDecimal.ZERO).multiply(BigDecimal.valueOf(units)));
        }
        String text = "Se registrarán " + count + " de " + rows.size() + " líneas · " + Money.format(cost) + " sin IVA";
        return invoice.total() == null ? text : text + " · total de la factura " + Money.format(invoice.total());
    }

    private static String orDash(String text) {
        return text == null || text.isBlank() ? "—" : text;
    }
}
