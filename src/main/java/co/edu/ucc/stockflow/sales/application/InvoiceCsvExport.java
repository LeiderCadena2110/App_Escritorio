package co.edu.ucc.stockflow.sales.application;

import co.edu.ucc.stockflow.sales.domain.Invoice;
import co.edu.ucc.stockflow.shared.util.CsvWriter;
import co.edu.ucc.stockflow.shared.util.Formats;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** Exports invoices to a CSV file for Excel (columns in Spanish, as the user sees them). */
@Component
public class InvoiceCsvExport {

    public void export(List<Invoice> invoices, Path file) throws IOException {
        CsvWriter.write(file, invoices, List.of(
                new CsvWriter.Column<Invoice>("Número", Invoice::getNumber),
                new CsvWriter.Column<Invoice>("Fecha", i -> Formats.dateTime(i.getIssuedAt())),
                new CsvWriter.Column<Invoice>("Cliente", Invoice::getCustomerName),
                new CsvWriter.Column<Invoice>("Documento", Invoice::getCustomerDocument),
                new CsvWriter.Column<Invoice>("Subtotal", Invoice::getSubtotal),
                new CsvWriter.Column<Invoice>("Descuento", Invoice::getDiscount),
                new CsvWriter.Column<Invoice>("IVA", Invoice::getTax),
                new CsvWriter.Column<Invoice>("Total", Invoice::getTotal),
                new CsvWriter.Column<Invoice>("Forma de pago", i -> i.getPaymentMethod().label()),
                new CsvWriter.Column<Invoice>("Estado", i -> i.getStatus().label())));
    }
}
