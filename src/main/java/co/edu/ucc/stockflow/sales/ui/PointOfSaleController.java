package co.edu.ucc.stockflow.sales.ui;

import co.edu.ucc.stockflow.catalog.application.ProductService;
import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.customers.application.CustomerService;
import co.edu.ucc.stockflow.customers.domain.Customer;
import co.edu.ucc.stockflow.customers.ui.CustomerDialog;
import co.edu.ucc.stockflow.sales.application.InvoiceDocumentExporter;
import co.edu.ucc.stockflow.sales.application.InvoiceService;
import co.edu.ucc.stockflow.sales.application.TaxPolicyProvider;
import co.edu.ucc.stockflow.sales.domain.Invoice;
import co.edu.ucc.stockflow.sales.domain.InvoiceItem;
import co.edu.ucc.stockflow.sales.domain.PaymentMethod;
import co.edu.ucc.stockflow.sales.domain.tax.TaxPolicy;
import co.edu.ucc.stockflow.settings.application.SequenceService;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.ui.Dialogs;
import co.edu.ucc.stockflow.shared.ui.Icon;
import co.edu.ucc.stockflow.shared.ui.Navigator;
import co.edu.ucc.stockflow.shared.ui.SectionController;
import co.edu.ucc.stockflow.shared.ui.TaskRunner;
import co.edu.ucc.stockflow.shared.ui.Toasts;
import co.edu.ucc.stockflow.shared.ui.ViewLoader;
import co.edu.ucc.stockflow.shared.ui.component.AutoCompleteField;
import co.edu.ucc.stockflow.shared.ui.component.FileChoosers;
import co.edu.ucc.stockflow.shared.ui.component.Tables;
import co.edu.ucc.stockflow.shared.util.Money;
import co.edu.ucc.stockflow.shared.util.TextSearch;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Point of sale screen. The cart rules and totals live in {@link SaleViewModel}; this class only
 * connects the controls to it, searches products and customers, and issues the invoice.
 */
@Component
@Scope("prototype")
public class PointOfSaleController implements SectionController {

    @FXML private Label nextNumberLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Product> resultsTable;
    @FXML private TableView<InvoiceItem> cartTable;
    @FXML private Label itemsLabel;
    @FXML private Button plusButton;
    @FXML private Button minusButton;
    @FXML private Button quantityButton;
    @FXML private Button removeButton;
    @FXML private HBox customerRow;
    @FXML private Label customerDetail;
    @FXML private ComboBox<PaymentMethod> paymentBox;
    @FXML private TextField discountField;
    @FXML private Label receivedLabel;
    @FXML private TextField receivedField;
    @FXML private FlowPane cashButtons;
    @FXML private TextField notesField;
    @FXML private Label subtotalLabel;
    @FXML private Label discountLabel;
    @FXML private Label taxNameLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label changeNameLabel;
    @FXML private Label changeLabel;
    @FXML private Label warningLabel;
    @FXML private Button chargeButton;
    @FXML private Button clearButton;
    @FXML private Button newCustomerButton;

    private final ProductService products;
    private final CustomerService customers;
    private final InvoiceService invoices;
    private final InvoiceDocumentExporter pdf;
    private final SequenceService sequences;
    private final TaxPolicyProvider taxPolicies;
    private final CustomerDialog customerDialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final ViewLoader views;
    private final FileChoosers files;
    private final TaskRunner tasks;
    private final Toasts toasts;

    private final SaleViewModel sale = new SaleViewModel();
    private final ObservableList<Product> catalog = FXCollections.observableArrayList();
    private final FilteredList<Product> results = new FilteredList<>(catalog);
    private final ObservableList<Customer> allCustomers = FXCollections.observableArrayList();
    private final AutoCompleteField<Customer> customerField = new AutoCompleteField<>(allCustomers,
            Customer::getName, PointOfSaleController::customerDocument,
            (c, text) -> TextSearch.matches(text, c.getName(), c.getDocumentNumber(), c.getPhone()));
    private final BooleanProperty charging = new SimpleBooleanProperty(false);
    private boolean shortcutsInstalled;
    private Customer finalConsumer;

    public PointOfSaleController(ProductService products, CustomerService customers, InvoiceService invoices,
                                 InvoiceDocumentExporter pdf, SequenceService sequences, TaxPolicyProvider taxPolicies,
                                 CustomerDialog customerDialog, Dialogs dialogs, Navigator navigator,
                                 ViewLoader views, FileChoosers files, TaskRunner tasks, Toasts toasts) {
        this.products = products;
        this.customers = customers;
        this.invoices = invoices;
        this.pdf = pdf;
        this.sequences = sequences;
        this.taxPolicies = taxPolicies;
        this.customerDialog = customerDialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
        this.views = views;
        this.files = files;
        this.tasks = tasks;
        this.toasts = toasts;
    }

    @FXML
    private void initialize() {
        addIcons();
        buildResultsTable();
        buildCartTable();
        bindInputs();
        bindTotals();

        searchField.textProperty().addListener((o, a, text) -> filterProducts(text));
        searchField.setOnAction(e -> addFromSearch());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                resultsTable.requestFocus();
                resultsTable.getSelectionModel().selectFirst();
            }
        });
        buildCustomerField();

        // F2: search, F12: charge. The filter is added once; it only acts while this view is on screen.
        searchField.sceneProperty().addListener((o, old, scene) -> {
            if (scene != null && !shortcutsInstalled) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::shortcuts);
                shortcutsInstalled = true;
            }
        });
    }

    private void addIcons() {
        quantityButton.setGraphic(Icon.EDIT.node(14));
        removeButton.setGraphic(Icon.DELETE.node(14));
        clearButton.setGraphic(Icon.CLOSE.node(14));
        newCustomerButton.setGraphic(Icon.PERSON_ADD.node(16));
        chargeButton.setGraphic(Icon.CHECK_CIRCLE.node(20));
        chargeButton.setGraphicTextGap(10);
    }

    private void buildResultsTable() {
        resultsTable.setItems(results);
        resultsTable.getColumns().setAll(List.of(
                Tables.text("Código", Product::getSku, 100),
                Tables.text("Producto", Product::getName, 260),
                Tables.text("Categoría", Product::getCategoryName, 120),
                Tables.money("Precio", Product::getSalePrice, 110),
                Tables.integer("Stock", Product::getStock, 70)));
        Tables.fill(resultsTable, 1);
        Tables.placeholder(resultsTable, "No hay productos que coincidan con la búsqueda.");
        Tables.styledRows(resultsTable, p -> p.getStock() == 0 ? "row-inactive" : p.isLowStock() ? "row-low-stock" : null,
                this::addProduct);
    }

    private void buildCartTable() {
        cartTable.setItems(sale.getItems());
        cartTable.getColumns().setAll(List.of(
                Tables.text("Código", InvoiceItem::getProductSku, 100),
                Tables.text("Producto", InvoiceItem::getProductName, 260),
                Tables.integer("Cant.", InvoiceItem::getQuantity, 70),
                Tables.money("Precio", InvoiceItem::getUnitPrice, 110),
                Tables.money("Total", InvoiceItem::getLineTotal, 120)));
        Tables.fill(cartTable, 1);
        Tables.placeholder(cartTable, "Busque un producto y pulse Enter o haga doble clic para agregarlo.");
        cartTable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) removeItem();
            else if (e.getCode() == KeyCode.ADD || e.getCode() == KeyCode.PLUS) increase();
            else if (e.getCode() == KeyCode.SUBTRACT || e.getCode() == KeyCode.MINUS) decrease();
        });
        var noItem = cartTable.getSelectionModel().selectedItemProperty().isNull();
        plusButton.disableProperty().bind(noItem);
        minusButton.disableProperty().bind(noItem);
        quantityButton.disableProperty().bind(noItem);
        removeButton.disableProperty().bind(noItem);
    }

    /** Controls → view model. */
    private void bindInputs() {
        paymentBox.getItems().setAll(PaymentMethod.values());
        paymentBox.valueProperty().bindBidirectional(sale.paymentMethodProperty());
        customerField.valueProperty().bindBidirectional(sale.customerProperty());
        notesField.textProperty().bindBidirectional(sale.notesProperty());
        moneyField(discountField, sale.discountProperty()::set);
        moneyField(receivedField, sale.receivedProperty()::set);
    }

    private static void moneyField(TextField field, Consumer<BigDecimal> target) {
        field.textProperty().addListener((o, a, text) -> target.accept(Money.parseOr(text, BigDecimal.ZERO)));
    }

    /** View model → labels and buttons. */
    private void bindTotals() {
        subtotalLabel.textProperty().bind(money(sale.subtotalProperty()));
        discountLabel.textProperty().bind(Bindings.createStringBinding(() -> sale.getAppliedDiscount().signum() > 0
                ? "- " + Money.format(sale.getAppliedDiscount()) : Money.format(BigDecimal.ZERO),
                sale.appliedDiscountProperty()));
        taxNameLabel.textProperty().bind(sale.taxLabelProperty());
        taxLabel.textProperty().bind(money(sale.taxProperty()));
        totalLabel.textProperty().bind(money(sale.totalProperty()));
        changeLabel.textProperty().bind(money(sale.changeProperty()));
        warningLabel.textProperty().bind(sale.warningProperty());
        itemsLabel.textProperty().bind(Bindings.createStringBinding(() -> sale.getItems().size() + " productos · "
                + sale.getUnits() + " unidades", sale.getItems(), sale.unitsProperty()));

        var notCash = sale.cashProperty().not();
        receivedLabel.disableProperty().bind(notCash);
        receivedField.disableProperty().bind(notCash);
        changeNameLabel.visibleProperty().bind(sale.cashProperty());
        changeLabel.visibleProperty().bind(sale.cashProperty());
        chargeButton.disableProperty().bind(sale.emptyProperty().or(charging));

        cashButtons.visibleProperty().bind(sale.cashProperty().and(sale.emptyProperty().not()));
        cashButtons.managedProperty().bind(cashButtons.visibleProperty());
        sale.totalProperty().addListener((o, a, total) -> showCashButtons(total));
    }

    /** "Exacto" and the next bills: one click fills "Recibido" and shows the change. */
    private void showCashButtons(BigDecimal total) {
        List<BigDecimal> amounts = CashSuggestions.forTotal(total);
        cashButtons.getChildren().clear();
        for (int i = 0; i < amounts.size(); i++) {
            BigDecimal amount = amounts.get(i);
            Button b = new Button(i == 0 ? "Exacto" : Money.format(amount));
            b.getStyleClass().add("cash-button");
            b.setFocusTraversable(false);
            if (i == 0) b.setTooltip(new Tooltip(Money.format(amount)));
            b.setOnAction(e -> receivedField.setText(Money.number(amount)));
            cashButtons.getChildren().add(b);
        }
    }

    private static StringBinding money(ObservableValue<BigDecimal> value) {
        return Bindings.createStringBinding(() -> Money.format(value.getValue()), value);
    }

    private void shortcuts(KeyEvent e) {
        if (searchField.getScene() == null) return; // another section is showing
        if (e.getCode() == KeyCode.F2) {
            searchField.requestFocus();
            e.consume();
        } else if (e.getCode() == KeyCode.F3) {
            customerField.requestFocus();
            e.consume();
        } else if (e.getCode() == KeyCode.F12) {
            charge();
            e.consume();
        }
    }

    @Override
    public void onShow() {
        reloadData(null);
        Platform.runLater(searchField::requestFocus);
    }

    /** Everything the screen needs from the database, loaded in the background. */
    private record Loaded(List<Product> catalog, List<Customer> customers, Customer finalConsumer,
                          String nextNumber, TaxPolicy taxPolicy) {
    }

    /** Reloads products (stock), customers, VAT and the next invoice number; selects {@code customer} if given. */
    private void reloadData(Customer customer) {
        tasks.run("No se pudieron cargar los productos",
                () -> new Loaded(products.findActive(), customers.search(null), customers.finalConsumer(),
                        sequences.peekInvoiceNumber(), taxPolicies.current()),
                loaded -> render(loaded, customer));
    }

    private void render(Loaded loaded, Customer select) {
        catalog.setAll(loaded.catalog());
        sale.updateCatalog(loaded.catalog());
        sale.taxPolicyProperty().set(loaded.taxPolicy());
        filterProducts(searchField.getText());
        nextNumberLabel.setText("Próxima factura: " + loaded.nextNumber());

        Customer current = select != null ? select : customerField.getValue();
        Customer target = current != null ? current : loaded.finalConsumer();
        allCustomers.setAll(loaded.customers());
        customerField.select(allCustomers, target);
        finalConsumer = loaded.finalConsumer();
    }

    private void filterProducts(String text) {
        String t = text == null ? "" : text.trim();
        results.setPredicate(p -> t.equals(p.getBarcode())
                || TextSearch.matches(t, p.getSku(), p.getName(), p.getCategoryName()));
    }

    /** One box to find the customer by name, document or phone (F3 jumps to it). */
    private void buildCustomerField() {
        customerField.setPromptText("Buscar cliente por nombre o documento (F3)");
        customerField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(customerField, Priority.ALWAYS);
        customerRow.getChildren().add(0, customerField);
        customerDetail.textProperty().bind(Bindings.createStringBinding(() -> {
            Customer c = customerField.getValue();
            if (c == null) return "";
            if (c.isFinalConsumer()) return "Venta sin identificar al cliente. Para venta a crédito elija un cliente.";
            String phone = c.getPhone() == null || c.getPhone().isBlank() ? "" : " · Tel. " + c.getPhone();
            return customerDocument(c) + phone;
        }, customerField.valueProperty()));
    }

    private static String customerDocument(Customer c) {
        return c.getDocumentType() == null ? c.getDocumentNumber() : c.getDocumentType() + " " + c.getDocumentNumber();
    }

    /** Enter in the search box (or a barcode scanner): exact SKU or barcode first, otherwise the first result. */
    private void addFromSearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) return;
        Product chosen = catalog.stream()
                .filter(p -> p.getSku().equalsIgnoreCase(text) || text.equals(p.getBarcode()))
                .findFirst()
                .orElse(results.isEmpty() ? null : results.get(0));
        if (chosen == null) {
            sale.showNotice("No se encontró \"" + text + "\".");
            searchField.selectAll();
            return;
        }
        addProduct(chosen);
        searchField.clear();
    }

    /** Products opens this section with a product to sell: it goes into the cart. */
    @Override
    public void receive(Object request) {
        if (request instanceof Product product) addProduct(product);
    }

    /** Adds one unit of the product (or one more if it is already in the sale). */
    public void addProduct(Product product) {
        try {
            cartTable.getSelectionModel().select(sale.add(product));
        } catch (BusinessException e) {
            sale.showNotice(e.getMessage());
        }
    }

    private void onSelected(Consumer<InvoiceItem> action) {
        InvoiceItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item == null) return;
        try {
            action.accept(item);
            if (sale.getItems().contains(item)) cartTable.getSelectionModel().select(item);
        } catch (BusinessException e) {
            sale.showNotice(e.getMessage());
        }
    }

    @FXML
    private void increase() {
        onSelected(sale::increase);
    }

    @FXML
    private void decrease() {
        onSelected(sale::decrease);
    }

    @FXML
    private void removeItem() {
        onSelected(sale::remove);
    }

    @FXML
    private void changeQuantity() {
        InvoiceItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item == null) return;
        dialogs.askText(navigator.window(), "Cantidad de " + item.getProductName(), "Cantidad:",
                Integer.toString(item.getQuantity())).ifPresent(text -> {
            try {
                int quantity = Integer.parseInt(text.trim());
                onSelected(i -> sale.setQuantity(i, quantity));
            } catch (NumberFormatException e) {
                sale.showNotice("\"" + text + "\" no es una cantidad válida.");
            }
        });
    }

    @FXML
    private void clearSale() {
        if (!sale.isEmpty() && !dialogs.confirm(navigator.window(), "Vaciar venta",
                "¿Quitar todos los productos de la venta actual?", "Vaciar")) {
            return;
        }
        reset();
    }

    private void reset() {
        discountField.clear();
        receivedField.clear();
        sale.clear(finalConsumer);
    }

    @FXML
    private void newCustomer() {
        customerDialog.show(navigator.window(), null).ifPresent(this::reloadData);
    }

    @FXML
    private void charge() {
        if (sale.isEmpty() || charging.get()) return;
        charging.set(true);
        Invoice draft = sale.toInvoice();
        tasks.run("No se pudo facturar", () -> invoices.issue(draft), saved -> {
            charging.set(false);
            reset();
            reloadData(null);
            afterSale(saved);
            searchField.requestFocus();
        }, () -> {
            charging.set(false);
            reloadData(null); // the stock may have changed
        });
    }

    /**
     * A toast instead of a dialog: the next customer can be served at once. The change is in the
     * title because it is what the cashier needs to read; the PDF is one click away.
     */
    private void afterSale(Invoice saved) {
        String title;
        String detail;
        if (saved.getPaymentMethod() == PaymentMethod.CASH && saved.getChange().signum() > 0) {
            title = "Cambio: " + Money.format(saved.getChange());
            detail = "Factura " + saved.getNumber() + " · Total " + Money.format(saved.getTotal())
                    + " · Recibido " + Money.format(saved.getAmountPaid());
        } else {
            title = "Factura " + saved.getNumber() + " registrada";
            detail = "Total " + Money.format(saved.getTotal()) + " · " + saved.getPaymentMethod().label();
        }
        toasts.show(Toasts.Kind.SUCCESS, title, detail, "Guardar PDF", () -> savePdf(saved));
    }

    private void savePdf(Invoice saved) {
        Path file = files.savePdf(navigator.window(), saved.getNumber());
        if (file == null) return;
        tasks.run("No se pudo guardar el PDF",
                () -> pdf.export(invoices.findById(saved.getId()).orElse(saved), file),
                () -> views.openFile(file));
    }
}
