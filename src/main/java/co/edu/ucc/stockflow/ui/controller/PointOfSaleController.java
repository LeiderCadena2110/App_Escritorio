package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.service.CustomerService;
import co.edu.ucc.stockflow.service.InvoicePdfService;
import co.edu.ucc.stockflow.service.InvoiceService;
import co.edu.ucc.stockflow.service.ProductService;
import co.edu.ucc.stockflow.service.SequenceService;
import co.edu.ucc.stockflow.service.SettingsService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.TaskRunner;
import co.edu.ucc.stockflow.ui.ViewLoader;
import co.edu.ucc.stockflow.ui.component.FileChoosers;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.ui.dialog.CustomerDialog;
import co.edu.ucc.stockflow.ui.viewmodel.SaleViewModel;
import co.edu.ucc.stockflow.util.Money;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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
    @FXML private TextField customerSearch;
    @FXML private ComboBox<Customer> customerBox;
    @FXML private ComboBox<PaymentMethod> paymentBox;
    @FXML private TextField discountField;
    @FXML private Label receivedLabel;
    @FXML private TextField receivedField;
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

    private final ProductService products;
    private final CustomerService customers;
    private final InvoiceService invoices;
    private final InvoicePdfService pdf;
    private final SequenceService sequences;
    private final SettingsService settings;
    private final CustomerDialog customerDialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final ViewLoader views;
    private final FileChoosers files;
    private final TaskRunner tasks;

    private final SaleViewModel sale = new SaleViewModel();
    private final ObservableList<Product> catalog = FXCollections.observableArrayList();
    private final FilteredList<Product> results = new FilteredList<>(catalog);
    private final ObservableList<Customer> allCustomers = FXCollections.observableArrayList();
    private final FilteredList<Customer> customerOptions = new FilteredList<>(allCustomers);
    private final BooleanProperty charging = new SimpleBooleanProperty(false);
    private boolean shortcutsInstalled;
    private Customer finalConsumer;

    public PointOfSaleController(ProductService products, CustomerService customers, InvoiceService invoices,
                                 InvoicePdfService pdf, SequenceService sequences, SettingsService settings,
                                 CustomerDialog customerDialog, Dialogs dialogs, Navigator navigator,
                                 ViewLoader views, FileChoosers files, TaskRunner tasks) {
        this.products = products;
        this.customers = customers;
        this.invoices = invoices;
        this.pdf = pdf;
        this.sequences = sequences;
        this.settings = settings;
        this.customerDialog = customerDialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
        this.views = views;
        this.files = files;
        this.tasks = tasks;
    }

    @FXML
    private void initialize() {
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
        customerBox.setItems(customerOptions);
        customerSearch.textProperty().addListener((o, a, text) -> filterCustomers(text));

        // F2: search, F12: charge. The filter is added once; it only acts while this view is on screen.
        searchField.sceneProperty().addListener((o, old, scene) -> {
            if (scene != null && !shortcutsInstalled) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::shortcuts);
                shortcutsInstalled = true;
            }
        });
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
        customerBox.valueProperty().bindBidirectional(sale.customerProperty());
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
        taxNameLabel.textProperty().bind(Bindings.createStringBinding(() -> "IVA ("
                + sale.taxRateProperty().get().stripTrailingZeros().toPlainString() + " %)", sale.taxRateProperty()));
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
    }

    private static StringBinding money(ObservableValue<BigDecimal> value) {
        return Bindings.createStringBinding(() -> Money.format(value.getValue()), value);
    }

    private void shortcuts(KeyEvent e) {
        if (searchField.getScene() == null) return; // another section is showing
        if (e.getCode() == KeyCode.F2) {
            searchField.requestFocus();
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
                          String nextNumber, BigDecimal taxRate) {
    }

    /** Reloads products (stock), customers, VAT and the next invoice number; selects {@code customer} if given. */
    private void reloadData(Customer customer) {
        tasks.run("No se pudieron cargar los productos",
                () -> new Loaded(products.findActive(), customers.search(null), customers.finalConsumer(),
                        sequences.peekInvoiceNumber(), settings.taxRate()),
                loaded -> render(loaded, customer));
    }

    private void render(Loaded loaded, Customer select) {
        catalog.setAll(loaded.catalog());
        sale.updateCatalog(loaded.catalog());
        sale.taxRateProperty().set(loaded.taxRate());
        filterProducts(searchField.getText());
        nextNumberLabel.setText("Próxima factura: " + loaded.nextNumber());

        Customer current = select != null ? select : customerBox.getValue();
        Customer target = current != null ? current : loaded.finalConsumer();
        allCustomers.setAll(loaded.customers());
        filterCustomers(customerSearch.getText());
        allCustomers.stream().filter(c -> c.getId().equals(target.getId())).findFirst().ifPresent(customerBox::setValue);
        finalConsumer = loaded.finalConsumer();
    }

    private void filterProducts(String text) {
        String t = text == null ? "" : text.trim().toLowerCase();
        results.setPredicate(p -> t.isEmpty() || p.getSku().toLowerCase().contains(t)
                || p.getName().toLowerCase().contains(t)
                || (p.getBarcode() != null && p.getBarcode().equals(t))
                || (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(t)));
    }

    private void filterCustomers(String text) {
        String t = text == null ? "" : text.trim().toLowerCase();
        Customer selected = customerBox.getValue();
        customerOptions.setPredicate(c -> t.isEmpty() || c.getName().toLowerCase().contains(t)
                || c.getDocumentNumber().toLowerCase().contains(t));
        if (selected != null && customerOptions.contains(selected)) customerBox.setValue(selected);
        else if (!customerOptions.isEmpty() && !t.isEmpty()) customerBox.setValue(customerOptions.get(0));
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
        customerSearch.clear();
        sale.clear(finalConsumer);
    }

    @FXML
    private void newCustomer() {
        customerDialog.show(navigator.window(), null).ifPresent(c -> {
            customerSearch.clear();
            reloadData(c);
        });
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

    private void afterSale(Invoice saved) {
        StringBuilder msg = new StringBuilder("Total: ").append(Money.format(saved.getTotal()));
        if (saved.getPaymentMethod() == PaymentMethod.CASH) {
            msg.append("\nRecibido: ").append(Money.format(saved.getAmountPaid()))
                    .append("\nCambio: ").append(Money.format(saved.getChange()));
        } else {
            msg.append("\nForma de pago: ").append(saved.getPaymentMethod().label());
        }
        ButtonType savePdf = new ButtonType("Guardar PDF", ButtonBar.ButtonData.OTHER);
        ButtonType ok = new ButtonType("Nueva venta", ButtonBar.ButtonData.OK_DONE);
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg.toString(), savePdf, ok);
        dialogs.prepare(a, navigator.window(), "Factura " + saved.getNumber() + " registrada");
        if (a.showAndWait().filter(b -> b == savePdf).isPresent()) {
            Path file = files.savePdf(navigator.window(), saved.getNumber());
            if (file != null) {
                tasks.run("No se pudo guardar el PDF",
                        () -> pdf.export(invoices.findById(saved.getId()).orElse(saved), file),
                        () -> views.openFile(file));
            }
        }
    }
}
