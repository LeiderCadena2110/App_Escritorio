package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.model.Invoice;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Port: turns an invoice into a document file. Today the adapter writes a letter-size PDF; a thermal
 * printer receipt or an electronic invoice (DIAN) would be other adapters of the same port.
 */
public interface InvoiceDocumentExporter {

    void export(Invoice invoice, Path file) throws IOException;
}
