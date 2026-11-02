package co.edu.ucc.stockflow.repository;

import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.StockMovement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StockMovementRepository {

    private static final RowMapper<StockMovement> MAPPER = (rs, n) -> {
        StockMovement m = new StockMovement();
        m.setId(rs.getLong("id"));
        m.setProductId(rs.getLong("product_id"));
        m.setProductSku(rs.getString("sku"));
        m.setProductName(rs.getString("product_name"));
        m.setType(MovementType.valueOf(rs.getString("movement_type")));
        m.setQuantity(rs.getInt("quantity"));
        m.setStockAfter(rs.getInt("stock_after"));
        m.setUnitCost(rs.getBigDecimal("unit_cost"));
        m.setReference(rs.getString("reference"));
        m.setNotes(rs.getString("notes"));
        m.setCreatedAt(JdbcHelper.localDateTime(rs, "created_at"));
        return m;
    };

    private final JdbcTemplate jdbc;

    public StockMovementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StockMovement insert(StockMovement m) {
        if (m.getCreatedAt() == null) m.setCreatedAt(LocalDateTime.now());
        long id = JdbcHelper.insert(jdbc, """
                INSERT INTO stock_movements (product_id, movement_type, quantity, stock_after, unit_cost, reference,
                                             notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                m.getProductId(), m.getType().name(), m.getQuantity(), m.getStockAfter(), m.getUnitCost(),
                m.getReference(), m.getNotes(), JdbcHelper.timestamp(m.getCreatedAt()));
        m.setId(id);
        return m;
    }

    /** Movements between two dates (inclusive start, exclusive end), newest first. Null filters are skipped. */
    public List<StockMovement> search(LocalDateTime from, LocalDateTime to, Long productId, MovementType type) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.*, p.sku, p.name AS product_name
                FROM stock_movements m JOIN products p ON p.id = m.product_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (from != null) {
            sql.append(" AND m.created_at >= ?");
            args.add(JdbcHelper.timestamp(from));
        }
        if (to != null) {
            sql.append(" AND m.created_at < ?");
            args.add(JdbcHelper.timestamp(to));
        }
        if (productId != null) {
            sql.append(" AND m.product_id = ?");
            args.add(productId);
        }
        if (type != null) {
            sql.append(" AND m.movement_type = ?");
            args.add(type.name());
        }
        sql.append(" ORDER BY m.created_at DESC, m.id DESC");
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public List<StockMovement> findByProduct(long productId) {
        return search(null, null, productId, null);
    }
}
