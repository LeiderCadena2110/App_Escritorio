package co.edu.ucc.stockflow.repository;

import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.InvoiceStatus;
import co.edu.ucc.stockflow.model.PaymentMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepository {

    private static final String SELECT = """
            SELECT i.*, c.name AS customer_name, c.document_type, c.document_number
            FROM invoices i LEFT JOIN customers c ON c.id = i.customer_id
            """;

    private static final RowMapper<Invoice> MAPPER = (rs, n) -> {
        Invoice i = new Invoice();
        i.setId(rs.getLong("id"));
        i.setNumber(rs.getString("number"));
        i.setCustomerId(JdbcHelper.nullableLong(rs, "customer_id"));
        i.setCustomerName(rs.getString("customer_name"));
        String document = rs.getString("document_number");
        i.setCustomerDocument(document == null ? null : rs.getString("document_type") + " " + document);
        i.setIssuedAt(JdbcHelper.localDateTime(rs, "issued_at"));
        i.setSubtotal(rs.getBigDecimal("subtotal"));
        i.setDiscount(rs.getBigDecimal("discount"));
        i.setTaxRate(rs.getBigDecimal("tax_rate"));
        i.setTax(rs.getBigDecimal("tax"));
        i.setTotal(rs.getBigDecimal("total"));
        i.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        i.setAmountPaid(rs.getBigDecimal("amount_paid"));
        i.setStatus(InvoiceStatus.valueOf(rs.getString("status")));
        i.setNotes(rs.getString("notes"));
        i.setCancelledAt(JdbcHelper.localDateTime(rs, "cancelled_at"));
        return i;
    };

    private static final RowMapper<InvoiceItem> ITEM_MAPPER = (rs, n) -> {
        InvoiceItem item = new InvoiceItem();
        item.setId(rs.getLong("id"));
        item.setInvoiceId(rs.getLong("invoice_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setProductSku(rs.getString("product_sku"));
        item.setProductName(rs.getString("product_name"));
        item.setUnitCost(rs.getBigDecimal("unit_cost"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setQuantity(rs.getInt("quantity"));
        item.setLineTotal(rs.getBigDecimal("line_total"));
        return item;
    };

    private final JdbcTemplate jdbc;

    public InvoiceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Saves the invoice and all its lines. */
    public Invoice insert(Invoice inv) {
        long id = JdbcHelper.insert(jdbc, """
                INSERT INTO invoices (number, customer_id, issued_at, subtotal, discount, tax_rate, tax, total,
                                      payment_method, amount_paid, status, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                inv.getNumber(), inv.getCustomerId(), JdbcHelper.timestamp(inv.getIssuedAt()), inv.getSubtotal(),
                inv.getDiscount(), inv.getTaxRate(), inv.getTax(), inv.getTotal(), inv.getPaymentMethod().name(),
                inv.getAmountPaid(), inv.getStatus().name(), inv.getNotes());
        inv.setId(id);
        for (InvoiceItem item : inv.getItems()) {
            item.setInvoiceId(id);
            long itemId = JdbcHelper.insert(jdbc, """
                    INSERT INTO invoice_items (invoice_id, product_id, product_sku, product_name, quantity,
                                               unit_price, unit_cost, line_total)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, item.getProductId(), item.getProductSku(), item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.getUnitCost(), item.getLineTotal());
            item.setId(itemId);
        }
        return inv;
    }

    /** The invoice with its lines. */
    public Optional<Invoice> findById(long id) {
        Optional<Invoice> found = jdbc.query(SELECT + " WHERE i.id = ?", MAPPER, id).stream().findFirst();
        found.ifPresent(inv -> inv.setItems(findItems(id)));
        return found;
    }

    public List<InvoiceItem> findItems(long invoiceId) {
        return jdbc.query("SELECT * FROM invoice_items WHERE invoice_id = ? ORDER BY id", ITEM_MAPPER, invoiceId);
    }

    /** Invoices (without lines) between two dates, newest first. Null filters are skipped. */
    public List<Invoice> search(LocalDateTime from, LocalDateTime to, String text, InvoiceStatus status) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (from != null) {
            sql.append(" AND i.issued_at >= ?");
            args.add(JdbcHelper.timestamp(from));
        }
        if (to != null) {
            sql.append(" AND i.issued_at < ?");
            args.add(JdbcHelper.timestamp(to));
        }
        if (text != null && !text.isBlank()) {
            String like = JdbcHelper.likePattern(text);
            sql.append(" AND (LOWER(i.number) LIKE ? ESCAPE '\\' OR LOWER(COALESCE(c.name, '')) LIKE ? ESCAPE '\\'")
                    .append(" OR LOWER(COALESCE(c.document_number, '')) LIKE ? ESCAPE '\\')");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null) {
            sql.append(" AND i.status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY i.issued_at DESC, i.id DESC");
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public void markCancelled(long id, LocalDateTime when, String reason) {
        jdbc.update("""
                UPDATE invoices SET status = ?, cancelled_at = ?,
                       notes = TRIM(COALESCE(notes, '') || ' ' || ?)
                WHERE id = ?
                """, InvoiceStatus.CANCELLED.name(), JdbcHelper.timestamp(when), reason, id);
    }

    public boolean existsNumber(String number) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE number = ?", Integer.class, number) > 0;
    }
}
