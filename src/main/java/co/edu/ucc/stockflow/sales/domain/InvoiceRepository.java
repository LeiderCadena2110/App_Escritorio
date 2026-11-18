package co.edu.ucc.stockflow.sales.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Where issued invoices and their lines are stored.
 *
 * <p>Port: the application layer depends on this interface and JdbcInvoiceRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface InvoiceRepository {

    /** Saves the invoice and all its lines. */
    Invoice insert(Invoice inv);

    /** The invoice with its lines. */
    Optional<Invoice> findById(long id);

    List<InvoiceItem> findItems(long invoiceId);

    /** Invoices (without lines) between two dates, newest first. Null filters are skipped. */
    List<Invoice> search(LocalDateTime from, LocalDateTime to, String text, InvoiceStatus status);

    void markCancelled(long id, LocalDateTime when, String reason);

    boolean existsNumber(String number);
}
