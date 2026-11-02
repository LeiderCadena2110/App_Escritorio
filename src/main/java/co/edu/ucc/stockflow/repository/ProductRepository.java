package co.edu.ucc.stockflow.repository;

import co.edu.ucc.stockflow.model.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private static final String SELECT = """
            SELECT p.*, c.name AS category_name, s.name AS supplier_name
            FROM products p
            LEFT JOIN categories c ON c.id = p.category_id
            LEFT JOIN suppliers s ON s.id = p.supplier_id
            """;

    private static final RowMapper<Product> MAPPER = (rs, n) -> {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setCategoryId(JdbcHelper.nullableLong(rs, "category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setSupplierId(JdbcHelper.nullableLong(rs, "supplier_id"));
        p.setSupplierName(rs.getString("supplier_name"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setSalePrice(rs.getBigDecimal("sale_price"));
        p.setStock(rs.getInt("stock"));
        p.setMinStock(rs.getInt("min_stock"));
        p.setUnit(rs.getString("unit"));
        p.setActive(rs.getBoolean("active"));
        p.setCreatedAt(JdbcHelper.localDateTime(rs, "created_at"));
        p.setUpdatedAt(JdbcHelper.localDateTime(rs, "updated_at"));
        return p;
    };

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Products that match the text (SKU or name), the category and the stock filter.
     * Any argument may be null/false to skip that filter.
     */
    public List<Product> search(String text, Long categoryId, boolean lowStockOnly, boolean includeInactive) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            sql.append(" AND (LOWER(p.sku) LIKE ? ESCAPE '\\' OR LOWER(p.name) LIKE ? ESCAPE '\\')");
            String like = JdbcHelper.likePattern(text);
            args.add(like);
            args.add(like);
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            args.add(categoryId);
        }
        if (lowStockOnly) sql.append(" AND p.stock <= p.min_stock");
        if (!includeInactive) sql.append(" AND p.active = TRUE");
        sql.append(" ORDER BY p.name");
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public List<Product> findActive() {
        return search(null, null, false, false);
    }

    public List<Product> findLowStock() {
        return search(null, null, true, false);
    }

    public Optional<Product> findById(long id) {
        return jdbc.query(SELECT + " WHERE p.id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<Product> findBySku(String sku) {
        return jdbc.query(SELECT + " WHERE LOWER(p.sku) = LOWER(?)", MAPPER, sku.trim()).stream().findFirst();
    }

    public Product insert(Product p) {
        LocalDateTime now = LocalDateTime.now();
        long id = JdbcHelper.insert(jdbc, """
                INSERT INTO products (sku, name, description, category_id, supplier_id, cost_price, sale_price,
                                      stock, min_stock, unit, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                p.getSku(), p.getName(), p.getDescription(), p.getCategoryId(), p.getSupplierId(),
                p.getCostPrice(), p.getSalePrice(), p.getStock(), p.getMinStock(), p.getUnit(), p.isActive(),
                JdbcHelper.timestamp(now), JdbcHelper.timestamp(now));
        p.setId(id);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }

    /** Updates the product data. The stock is NOT changed here: it only changes through stock movements. */
    public void update(Product p) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE products SET sku = ?, name = ?, description = ?, category_id = ?, supplier_id = ?,
                       cost_price = ?, sale_price = ?, min_stock = ?, unit = ?, active = ?, updated_at = ?
                WHERE id = ?
                """,
                p.getSku(), p.getName(), p.getDescription(), p.getCategoryId(), p.getSupplierId(),
                p.getCostPrice(), p.getSalePrice(), p.getMinStock(), p.getUnit(), p.isActive(),
                JdbcHelper.timestamp(now), p.getId());
        p.setUpdatedAt(now);
    }

    public void updateStock(long id, int newStock) {
        jdbc.update("UPDATE products SET stock = ?, updated_at = ? WHERE id = ?",
                newStock, JdbcHelper.timestamp(LocalDateTime.now()), id);
    }

    public void updateCostPrice(long id, BigDecimal costPrice) {
        jdbc.update("UPDATE products SET cost_price = ? WHERE id = ?", costPrice, id);
    }

    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE products SET active = ?, updated_at = ? WHERE id = ?",
                active, JdbcHelper.timestamp(LocalDateTime.now()), id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM stock_movements WHERE product_id = ?", id);
        jdbc.update("DELETE FROM products WHERE id = ?", id);
    }

    public int countSales(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM invoice_items WHERE product_id = ?", Integer.class, id);
    }

    public int countActive() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE active = TRUE", Integer.class);
    }

    public int countLowStock() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE active = TRUE AND stock <= min_stock", Integer.class);
    }

    /** Value of all the units in stock at cost price. */
    public BigDecimal totalStockValue() {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(cost_price * stock), 0) FROM products WHERE active = TRUE", BigDecimal.class);
    }

    public int totalUnits() {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(stock), 0) FROM products WHERE active = TRUE", Integer.class);
    }
}
