package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.service.CustomerService;
import co.edu.ucc.stockflow.service.InvoicePdfService;
import co.edu.ucc.stockflow.service.InvoiceService;
import co.edu.ucc.stockflow.service.ProductService;
import co.edu.ucc.stockflow.service.SettingsService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.ViewLoader;
import co.edu.ucc.stockflow.ui.component.FileChoosers;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.ui.dialog.CustomerDialog;
import co.edu.ucc.stockflow.util.Money;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
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

/** Point of sale: builds the cart and issues the invoice. */
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
    private final SettingsService settings;
    private final CustomerDialog customerDialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final ViewLoader views;

    private final ObservableList<Product> catalog = FXCollections.observableArrayList();
    private final FilteredList<Product> results = new FilteredList<>(catalog);
    private final ObservableList<Customer> allCustomers = FXCollections.observableArrayList();
    private final FilteredList<Customer> customerOptions = new FilteredList<>(allCustomers);
    private final ObservableList<InvoiceItem> cart = FXCollections.observableArrayList();
    private Invoice preview = new Invoice();
    private boolean shortcutsInstalled;

    public PointOfSaleController(ProductService products, CustomerService customers, InvoiceService invoices,
                                 InvoicePdfService pdf, SettingsService settings, CustomerDialog customerDialog,
                                 Dialogs dialogs, Navigator navigator, ViewLoader views) {
        this.products = products;
        this.customers = customers;
        this.invoices = invoices;
        this.pdf = pdf;
        this.settings = settings;
        this.customerDialog = customerDialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
        this.views = views;
    }

    @FXML
    private void initialize() {
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

        cartTable.setItems(cart);
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

        paymentBox.getItems().setAll(PaymentMethod.values());
        paymentBox.setValue(PaymentMethod.CASH);
        paymentBox.valueProperty().addListener((o, a, b) -> updateTotals());
        discountField.textProperty().addListener((o, a, b) -> updateTotals());
        receivedField.textProperty().addListener((o, a, b) -> updateTotals());
        cart.addListener((javafx.collections.ListChangeListener<InvoiceItem>) c -> updateTotals());

        // F2: search, F12: charge. The filter is added once; it only acts while this view is on screen.
        searchField.sceneProperty().addListener((o, old, scene) -> {
            if (scene != null && !shortcutsInstalled) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::shortcuts);
                shortcutsInstalled = true;
            }
        });
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
        reloadCatalog();
        reloadCustomers(null);
        updateTotals();
        Platform.runLater(searchField::requestFocus);
    }

    private void reloadCatalog() {
        catalog.setAll(products.findActive());
        filterProducts(searchField.getText());
        nextNumberLabel.setText("Próxima factura: "
                + SettingsService.formatInvoiceNumber(settings.invoicePrefix(), settings.nextInvoiceNumber()));
    }

    private void reloadCustomers(Customer select) {
        Customer current = select != null ? select : customerBox.getValue();
        Customer finalConsumer = customers.finalConsumer();
        allCustomers.setAll(customers.search(null));
        filterCustomers(customerSearch.getText());
        Customer target = current != null ? current : finalConsumer;
        allCustomers.stream().filter(c -> c.getId().equals(target.getId())).findFirst().ifPresent(customerBox::setValue);
    }

    private void filterProducts(String text) {
        String t = text == null ? "" : text.trim().toLowerCase();
        results.setPredicate(p -> t.isEmpty() || p.getSku().toLowerCase().contains(t)
                || p.getName().toLowerCase().contains(t)
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

    /** Enter in the search box: exact code first, otherwise the only (or first) result. */
    private void addFromSearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) return;
        Optional<Product> exact = catalog.stream().filter(p -> p.getSku().equalsIgnoreCase(text)).findFirst();
        Product chosen = exact.orElse(results.isEmpty() ? null : results.get(0));
        if (chosen == null) {
            warningLabel.setText("No se encontró \"" + text + "\".");
            return;
        }
        addProduct(chosen);
        searchField.clear();
    }

    /** Adds one unit of the product (or one more if it is already in the cart). */
    public void addProduct(Product product) {
        Product p = catalog.stream().filter(x -> x.getId().equals(product.getId())).findFirst().orElse(product);
        Optional<InvoiceItem> existing = cart.stream().filter(i -> i.getProductId().equals(p.getId())).findFirst();
        int wanted = existing.map(i -> i.getQuantity() + 1).orElse(1);
        if (wanted > p.getStock()) {
            warningLabel.setText(p.getStock() == 0 ? p.getName() + " está agotado."
                    : "Solo hay " + p.getStock() + " " + p.getUnit() + " de " + p.getName() + ".");
            return;
        }
        if (existing.isPresent()) {
            existing.get().setQuantity(wanted);
            cartTable.refresh();
            cartTable.getSelectionModel().select(existing.get());
        } else {
            InvoiceItem item = InvoiceItem.of(p, 1);
            cart.add(item);
            cartTable.getSelectionModel().select(item);
        }
        updateTotals();
    }

    @FXML
    private void increase() {
        InvoiceItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item == null) return;
        setQuantity(item, item.getQuantity() + 1);
    }

    @FXML
    private void decrease() {
        InvoiceItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item == null) return;
        if (item.getQuantity() <= 1) removeItem();
        else setQuantity(item, item.getQuantity() - 1);
    }

    @FXML
    private void changeQuantity() {
        InvoiceItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item == null) return;
        dialogs.askText(navigator.window(), "Cantidad de " + item.getProductName(), "Cantidad:",
                Integer.toString(item.getQuantity())).ifPresent(text -> {
            try {
                int q = Integer.parseInt(text.trim());
                if (q <= 0) removeItem();
                else setQuantity(item, q);
            } catch (NumberFormatException e) {
                warningLabel.setText("\"" + text + "\" no es una cantidad válida.");
            }
        });
    }

    private void setQuantity(InvoiceItem item, int quantity) {
        int stock = catalog.stream().filter(p -> p.getId().equals(item.getProductId()))
                .mapToInt(Product::getStock).findFirst().orElse(Integer.MAX_VALUE);
        if (quantity > stock) {
            warningLabel.setText("Solo hay " + stock + " unidades de " + item.getProductName() + ".");
            return;
        }
        item.setQuantity(quantity);
        cartTable.refresh();
        updateTotals();
    }

    @FXML
    private void removeItem() {
        InvoiceItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item != null) cart.remove(item);
    }

    @FXML
    private void clearSale() {
        if (!cart.isEmpty() && !dialogs.confirm(navigator.window(), "Vaciar venta",
                "¿Quitar todos los productos de la venta actual?", "Vaciar")) {
            return;
        }
        reset();
    }

    private void reset() {
        cart.clear();
        discountField.clear();
        receivedField.clear();
        notesField.clear();
        customerSearch.clear();
        paymentBox.setValue(PaymentMethod.CASH);
        reloadCustomers(customers.finalConsumer());
        warningLabel.setText("");
        updateTotals();
    }

    @FXML
    private void newCustomer() {
        customerDialog.show(navigator.window(), null).ifPresent(c -> {
            customerSearch.clear();
            reloadCustomers(c);
        });
    }

    /** Recalculates the totals shown on the right panel. */
    private void updateTotals() {
        preview = buildInvoice();
        invoices.calculateTotals(preview);
        String rate = preview.getTaxRate().stripTrailingZeros().toPlainString();
        subtotalLabel.setText(Money.format(preview.getSubtotal()));
        discountLabel.setText(preview.getDiscount().signum() > 0 ? "- " + Money.format(preview.getDiscount()) : Money.format(BigDecimal.ZERO));
        taxNameLabel.setText("IVA (" + rate + " %)");
        taxLabel.setText(Money.format(preview.getTax()));
        totalLabel.setText(Money.format(preview.getTotal()));

        boolean cash = paymentBox.getValue() == PaymentMethod.CASH;
        receivedLabel.setDisable(!cash);
        receivedField.setDisable(!cash);
        changeNameLabel.setVisible(cash);
        changeLabel.setVisible(cash);
        BigDecimal received = Money.parseOr(receivedField.getText(), BigDecimal.ZERO);
        String warning = "";
        if (cash && received.signum() > 0) {
            BigDecimal change = received.subtract(preview.getTotal());
            changeLabel.setText(Money.format(change.signum() < 0 ? BigDecimal.ZERO : change));
            if (change.signum() < 0) warning = "Faltan " + Money.format(change.negate()) + " para completar el pago.";
        } else {
            changeLabel.setText(Money.format(BigDecimal.ZERO));
        }
        BigDecimal discount = Money.parseOr(discountField.getText(), BigDecimal.ZERO);
        if (discount.compareTo(preview.getSubtotal()) > 0 && !cart.isEmpty()) {
            warning = "El descuento es mayor que el subtotal.";
        }
        warningLabel.setText(warning);
        int units = cart.stream().mapToInt(InvoiceItem::getQuantity).sum();
        itemsLabel.setText(cart.size() + " productos · " + units + " unidades");
        chargeButton.setDisable(cart.isEmpty());
    }

    private Invoice buildInvoice() {
        Invoice inv = new Invoice();
        for (InvoiceItem item : cart) {
            InvoiceItem copy = new InvoiceItem();
            copy.setProductId(item.getProductId());
            copy.setProductSku(item.getProductSku());
            copy.setProductName(item.getProductName());
            copy.setUnitCost(item.getUnitCost());
            copy.setUnitPrice(item.getUnitPrice());
            copy.setQuantity(item.getQuantity());
            inv.getItems().add(copy);
        }
        inv.setCustomerId(customerBox.getValue() == null ? null : customerBox.getValue().getId());
        inv.setPaymentMethod(paymentBox.getValue());
        inv.setDiscount(Money.parseOr(discountField.getText(), BigDecimal.ZERO));
        inv.setAmountPaid(Money.parseOr(receivedField.getText(), BigDecimal.ZERO));
        inv.setNotes(notesField.getText());
        return inv;
    }

    @FXML
    private void charge() {
        if (cart.isEmpty()) return;
        Invoice draft = buildInvoice();
        Invoice saved;
        try {
            saved = invoices.issue(draft);
        } catch (RuntimeException e) {
            dialogs.error(navigator.window(), "No se pudo facturar", e);
            reloadCatalog();
            return;
        }
        afterSale(saved);
        reset();
        reloadCatalog();
        searchField.requestFocus();
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
            Path file = FileChoosers.savePdf(navigator.window(), saved.getNumber());
            if (file != null) {
                try {
                    pdf.export(invoices.findById(saved.getId()).orElse(saved), file);
                    views.openFile(file);
                } catch (IOException | RuntimeException e) {
                    dialogs.error(navigator.window(), "No se pudo guardar el PDF", e);
                }
            }
        }
    }
}
