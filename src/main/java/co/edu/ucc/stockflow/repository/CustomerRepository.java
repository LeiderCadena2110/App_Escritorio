package co.edu.ucc.stockflow.repository;

import co.edu.ucc.stockflow.model.Customer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private static final RowMapper<Customer> MAPPER = (rs, n) -> {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setDocumentType(rs.getString("document_type"));
        c.setDocumentNumber(rs.getString("document_number"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setCreatedAt(JdbcHelper.localDateTime(rs, "created_at"));
        return c;
    };

    private final JdbcTemplate jdbc;

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Customer> search(String text) {
        if (text == null || text.isBlank()) {
            return jdbc.query("SELECT * FROM customers ORDER BY name", MAPPER);
        }
        String like = JdbcHelper.likePattern(text);
        return jdbc.query("""
                SELECT * FROM customers
                WHERE LOWER(name) LIKE ? ESCAPE '\\' OR LOWER(document_number) LIKE ? ESCAPE '\\'
                   OR LOWER(COALESCE(phone, '')) LIKE ? ESCAPE '\\'
                ORDER BY name
                """, MAPPER, like, like, like);
    }

    public Optional<Customer> findById(long id) {
        return jdbc.query("SELECT * FROM customers WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<Customer> findByDocument(String documentNumber) {
        return jdbc.query("SELECT * FROM customers WHERE document_number = ?", MAPPER, documentNumber.trim())
                .stream().findFirst();
    }

    public Customer save(Customer c) {
        if (c.getId() == null) {
            LocalDateTime now = LocalDateTime.now();
            long id = JdbcHelper.insert(jdbc, """
                    INSERT INTO customers (document_type, document_number, name, phone, email, address, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    c.getDocumentType(), c.getDocumentNumber(), c.getName(), c.getPhone(), c.getEmail(),
                    c.getAddress(), JdbcHelper.timestamp(now));
            c.setId(id);
            c.setCreatedAt(now);
        } else {
            jdbc.update("""
                    UPDATE customers SET document_type = ?, document_number = ?, name = ?, phone = ?, email = ?,
                           address = ?
                    WHERE id = ?
                    """,
                    c.getDocumentType(), c.getDocumentNumber(), c.getName(), c.getPhone(), c.getEmail(),
                    c.getAddress(), c.getId());
        }
        return c;
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM customers WHERE id = ?", id);
    }

    public int countInvoices(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE customer_id = ?", Integer.class, id);
    }

    public int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
    }
}
