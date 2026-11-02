package co.edu.ucc.stockflow.repository;

import co.edu.ucc.stockflow.model.Supplier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SupplierRepository {

    private static final RowMapper<Supplier> MAPPER = (rs, n) -> {
        Supplier s = new Supplier();
        s.setId(rs.getLong("id"));
        s.setTaxId(rs.getString("tax_id"));
        s.setName(rs.getString("name"));
        s.setContactName(rs.getString("contact_name"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setActive(rs.getBoolean("active"));
        s.setCreatedAt(JdbcHelper.localDateTime(rs, "created_at"));
        return s;
    };

    private final JdbcTemplate jdbc;

    public SupplierRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Supplier> search(String text) {
        if (text == null || text.isBlank()) {
            return jdbc.query("SELECT * FROM suppliers ORDER BY name", MAPPER);
        }
        String like = JdbcHelper.likePattern(text);
        return jdbc.query("""
                SELECT * FROM suppliers
                WHERE LOWER(name) LIKE ? ESCAPE '\\' OR LOWER(COALESCE(tax_id, '')) LIKE ? ESCAPE '\\'
                   OR LOWER(COALESCE(contact_name, '')) LIKE ? ESCAPE '\\'
                ORDER BY name
                """, MAPPER, like, like, like);
    }

    public List<Supplier> findActive() {
        return jdbc.query("SELECT * FROM suppliers WHERE active = TRUE ORDER BY name", MAPPER);
    }

    public Optional<Supplier> findById(long id) {
        return jdbc.query("SELECT * FROM suppliers WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Supplier save(Supplier s) {
        if (s.getId() == null) {
            LocalDateTime now = LocalDateTime.now();
            long id = JdbcHelper.insert(jdbc, """
                    INSERT INTO suppliers (tax_id, name, contact_name, phone, email, address, active, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    s.getTaxId(), s.getName(), s.getContactName(), s.getPhone(), s.getEmail(), s.getAddress(),
                    s.isActive(), JdbcHelper.timestamp(now));
            s.setId(id);
            s.setCreatedAt(now);
        } else {
            jdbc.update("""
                    UPDATE suppliers SET tax_id = ?, name = ?, contact_name = ?, phone = ?, email = ?, address = ?,
                           active = ?
                    WHERE id = ?
                    """,
                    s.getTaxId(), s.getName(), s.getContactName(), s.getPhone(), s.getEmail(), s.getAddress(),
                    s.isActive(), s.getId());
        }
        return s;
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM suppliers WHERE id = ?", id);
    }

    public int countProducts(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE supplier_id = ?", Integer.class, id);
    }
}
