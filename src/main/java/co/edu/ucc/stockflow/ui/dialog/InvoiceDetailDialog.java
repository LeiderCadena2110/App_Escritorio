package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.tax.TaxLine;
import co.edu.ucc.stockflow.shared.ui.Dialogs;
import co.edu.ucc.stockflow.shared.ui.component.Tables;
import co.edu.ucc.stockflow.shared.util.Formats;
import co.edu.ucc.stockflow.shared.util.Money;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Read-only view of an invoice with buttons to export it to PDF or cancel it. */
@Component
public class InvoiceDetailDialog {

    /** What the user chose in the dialog. */
    public enum Action { EXPORT_PDF, CANCEL_INVOICE }

    private final Dialogs dialogs;

    public InvoiceDetailDialog(Dialogs dialogs) {
        this.dialogs = dialogs;
    }

    public Optional<Action> show(Window owner, Invoice invoice) {
        Dialog<Action> d = new Dialog<>();
        dialogs.prepare(d, owner, "Factura " + invoice.getNumber());
        ButtonType pdf = new ButtonType("Exportar PDF", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Anular factura", ButtonBar.ButtonData.LEFT);
        ButtonType close = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().add(pdf);
        if (!invoice.isCancelled()) d.getDialogPane().getButtonTypes().add(cancel);
        d.getDialogPane().getButtonTypes().add(close);
        d.getDialogPane().lookupButton(pdf).getStyleClass().add("primary-button");
        if (!invoice.isCancelled()) d.getDialogPane().lookupButton(cancel).getStyleClass().add("danger-button");

        GridPane info = new GridPane();
        info.setHgap(14);
        info.setVgap(6);
        int r = 0;
        r = row(info, r, "Fecha", Formats.dateTime(invoice.getIssuedAt()));
        r = row(info, r, "Cliente", invoice.getCustomerName() + "  ·  " + Formats.blankIfNull(invoice.getCustomerDocument()));
        r = row(info, r, "Forma de pago", invoice.getPaymentMethod().label());
        r = row(info, r, "Estado", invoice.getStatus().label()
                + (invoice.isCancelled() ? " el " + Formats.dateTime(invoice.getCancelledAt()) : ""));
        if (invoice.getNotes() != null) row(info, r, "Notas", invoice.getNotes());

        TableView<InvoiceItem> items = new TableView<>();
        items.getColumns().setAll(List.of(
                Tables.text("Código", InvoiceItem::getProductSku, 90),
                Tables.text("Producto", InvoiceItem::getProductName, 240),
                Tables.integer("Cant.", InvoiceItem::getQuantity, 60),
                Tables.money("Precio", InvoiceItem::getUnitPrice, 110),
                Tables.money("Total", InvoiceItem::getLineTotal, 120)));
        Tables.fill(items, 1);
        items.getItems().setAll(invoice.getItems());
        items.setPrefHeight(Math.min(320, 40 + invoice.getItems().size() * 28.0));

        GridPane totals = new GridPane();
        totals.setHgap(16);
        totals.setVgap(4);
        int t = 0;
        t = total(totals, t, "Subtotal", Money.format(invoice.getSubtotal()), false);
        if (invoice.getDiscount().signum() > 0) t = total(totals, t, "Descuento", "- " + Money.format(invoice.getDiscount()), false);
        for (String[] vat : vatLines(invoice)) t = total(totals, t, vat[0], vat[1], false);
        t = total(totals, t, "Total", Money.format(invoice.getTotal()), true);
        if (invoice.getPaymentMethod() == PaymentMethod.CASH) {
            t = total(totals, t, "Recibido", Money.format(invoice.getAmountPaid()), false);
            total(totals, t, "Cambio", Money.format(invoice.getChange()), false);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(spacer, totals);

        VBox content = new VBox(14, info, items, bottom);
        content.setPrefWidth(640);
        d.getDialogPane().setContent(content);
        d.setResultConverter(b -> b == pdf ? Action.EXPORT_PDF : b == cancel ? Action.CANCEL_INVOICE : null);
        return d.showAndWait();
    }

    /** One line per VAT rate with tax ("IVA 19 % sobre $ 90.000"), or a single "IVA $ 0" line when nothing paid VAT. */
    private static java.util.List<String[]> vatLines(Invoice invoice) {
        java.util.List<String[]> lines = new java.util.ArrayList<>();
        for (TaxLine vat : invoice.getTaxBreakdown()) {
            if (vat.rate().signum() == 0) continue;
            lines.add(new String[]{"IVA " + vat.rateLabel() + " sobre " + Money.format(vat.base()), Money.format(vat.tax())});
        }
        if (lines.isEmpty()) lines.add(new String[]{"IVA", Money.format(invoice.getTax())});
        return lines;
    }

    private static int row(GridPane g, int r, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        Label v = new Label(value);
        v.setWrapText(true);
        g.addRow(r, l, v);
        return r + 1;
    }

    private static int total(GridPane g, int r, String label, String value, boolean strong) {
        Label l = new Label(label);
        l.getStyleClass().add(strong ? "grand-total-label" : "total-label");
        Label v = new Label(value);
        v.getStyleClass().add(strong ? "grand-total-value" : "total-value");
        GridPane.setHalignment(v, javafx.geometry.HPos.RIGHT);
        g.addRow(r, l, v);
        return r + 1;
    }
}
