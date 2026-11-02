package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceStatus;
import co.edu.ucc.stockflow.service.CsvExportService;
import co.edu.ucc.stockflow.service.InvoicePdfService;
import co.edu.ucc.stockflow.service.InvoiceService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.Section;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.ViewLoader;
import co.edu.ucc.stockflow.ui.component.Combos;
import co.edu.ucc.stockflow.ui.component.DatePickers;
import co.edu.ucc.stockflow.ui.component.FileChoosers;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.ui.dialog.InvoiceDetailDialog;
import co.edu.ucc.stockflow.util.DateRange;
import co.edu.ucc.stockflow.util.Money;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class InvoicesController implements SectionController {

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private TextField searchField;
    @FXML private ComboBox<InvoiceStatus> statusBox;
    @FXML private TableView<Invoice> table;
    @FXML private Button detailButton;
    @FXML private Button pdfButton;
    @FXML private Button cancelButton;
    @FXML private Label summaryLabel;

    private final InvoiceService invoices;
    private final InvoicePdfService pdf;
    private final CsvExportService csv;
    private final InvoiceDetailDialog detailDialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final ViewLoader views;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(250));

    public InvoicesController(InvoiceService invoices, InvoicePdfService pdf, CsvExportService csv,
                              InvoiceDetailDialog detailDialog, Dialogs dialogs, Navigator navigator,
                              ViewLoader views) {
        this.invoices = invoices;
        this.pdf = pdf;
        this.csv = csv;
        this.detailDialog = detailDialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
        this.views = views;
    }

    @FXML
    private void initialize() {
        table.getColumns().setAll(List.of(
                Tables.text("Número", Invoice::getNumber, 110),
                Tables.dateTime("Fecha", Invoice::getIssuedAt, 140),
                Tables.text("Cliente", Invoice::getCustomerName, 230),
                Tables.text("Documento", Invoice::getCustomerDocument, 140),
                Tables.text("Pago", i -> i.getPaymentMethod().label(), 110),
                Tables.money("Total", Invoice::getTotal, 130),
                Tables.badge("Estado", i -> i.getStatus().label(),
                        i -> i.isCancelled() ? "badge-danger" : "badge-ok", 100)));
        Tables.fill(table, 2);
        Tables.placeholder(table, "No hay facturas en ese periodo.");
        Tables.styledRows(table, i -> i.isCancelled() ? "row-cancelled" : null, this::open);

        var none = table.getSelectionModel().selectedItemProperty().isNull();
        detailButton.disableProperty().bind(none);
        pdfButton.disableProperty().bind(none);
        cancelButton.disableProperty().bind(none);

        statusBox.getItems().setAll((InvoiceStatus) null);
        statusBox.getItems().addAll(InvoiceStatus.values());
        Combos.nullText(statusBox, "Todos los estados");

        DatePickers.dayMonthYear(fromDate, toDate);
        DateRange month = DateRange.thisMonth();
        fromDate.setValue(month.start().toLocalDate());
        toDate.setValue(month.end().toLocalDate().minusDays(1));
        fromDate.valueProperty().addListener((o, a, b) -> reload());
        toDate.valueProperty().addListener((o, a, b) -> reload());
        statusBox.valueProperty().addListener((o, a, b) -> reload());
        searchDelay.setOnFinished(e -> reload());
        searchField.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
    }

    @Override
    public void onShow() {
        reload();
    }

    private void reload() {
        LocalDate from = fromDate.getValue() == null ? LocalDate.of(2000, 1, 1) : fromDate.getValue();
        LocalDate to = toDate.getValue() == null ? LocalDate.now().plusYears(1) : toDate.getValue();
        List<Invoice> list = invoices.search(DateRange.ofDays(from, to), searchField.getText(), statusBox.getValue());
        table.getItems().setAll(list);
        BigDecimal total = list.stream().filter(i -> !i.isCancelled()).map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long cancelled = list.stream().filter(Invoice::isCancelled).count();
        summaryLabel.setText(list.size() + " facturas · " + cancelled + " anuladas · total vendido " + Money.format(total));
    }

    @FXML
    private void today() {
        fromDate.setValue(LocalDate.now());
        toDate.setValue(LocalDate.now());
    }

    @FXML
    private void thisMonth() {
        DateRange month = DateRange.thisMonth();
        fromDate.setValue(month.start().toLocalDate());
        toDate.setValue(month.end().toLocalDate().minusDays(1));
    }

    @FXML
    private void newSale() {
        navigator.goTo(Section.POINT_OF_SALE);
    }

    @FXML
    private void openSelected() {
        Invoice i = table.getSelectionModel().getSelectedItem();
        if (i != null) open(i);
    }

    private void open(Invoice summary) {
        Invoice invoice = invoices.findById(summary.getId()).orElse(null);
        if (invoice == null) return;
        detailDialog.show(navigator.window(), invoice).ifPresent(action -> {
            switch (action) {
                case EXPORT_PDF -> exportPdf(invoice);
                case CANCEL_INVOICE -> cancel(invoice);
            }
        });
    }

    @FXML
    private void exportPdf() {
        Invoice i = table.getSelectionModel().getSelectedItem();
        if (i != null) invoices.findById(i.getId()).ifPresent(this::exportPdf);
    }

    private void exportPdf(Invoice invoice) {
        Path file = FileChoosers.savePdf(navigator.window(), invoice.getNumber());
        if (file == null) return;
        dialogs.run(navigator.window(), "No se pudo guardar el PDF", () -> {
            try {
                pdf.export(invoice, file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            views.openFile(file);
        });
    }

    @FXML
    private void cancelSelected() {
        Invoice i = table.getSelectionModel().getSelectedItem();
        if (i != null) cancel(i);
    }

    private void cancel(Invoice invoice) {
        if (invoice.isCancelled()) {
            dialogs.info(navigator.window(), "Factura anulada", "La factura " + invoice.getNumber() + " ya está anulada.");
            return;
        }
        dialogs.askText(navigator.window(), "Anular factura " + invoice.getNumber(),
                "Motivo de la anulación (los productos vuelven al inventario):", "").ifPresent(reason ->
                dialogs.run(navigator.window(), "No se pudo anular", () -> {
                    invoices.cancel(invoice.getId(), reason);
                    reload();
                }));
    }

    @FXML
    private void exportCsv() {
        Path file = FileChoosers.saveCsv(navigator.window(), "facturas");
        if (file == null) return;
        dialogs.run(navigator.window(), "No se pudo exportar", () -> {
            try {
                csv.exportInvoices(table.getItems(), file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            views.openFile(file);
        });
    }
}
