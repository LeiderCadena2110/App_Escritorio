package co.edu.ucc.stockflow.catalog.infrastructure;

import co.edu.ucc.stockflow.catalog.domain.Category;
import co.edu.ucc.stockflow.catalog.domain.CategoryRepository;
import co.edu.ucc.stockflow.shared.persistence.JdbcHelper;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCategoryRepository implements CategoryRepository {

    private static final RowMapper<Category> MAPPER = (rs, n) ->
            new Category(rs.getLong("id"), rs.getString("name"), rs.getString("description"));

    private final JdbcTemplate jdbc;

    public JdbcCategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Category> findAll() {
        return jdbc.query("SELECT * FROM categories ORDER BY name", MAPPER);
    }

    @Override
    public Optional<Category> findById(long id) {
        return jdbc.query("SELECT * FROM categories WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<Category> findByName(String name) {
        return jdbc.query("SELECT * FROM categories WHERE LOWER(name) = LOWER(?)", MAPPER, name.trim())
                .stream().findFirst();
    }

    @Override
    public Category save(Category category) {
        if (category.getId() == null) {
            long id = JdbcHelper.insert(jdbc, "INSERT INTO categories (name, description) VALUES (?, ?)",
                    category.getName(), category.getDescription());
            category.setId(id);
        } else {
            jdbc.update("UPDATE categories SET name = ?, description = ? WHERE id = ?",
                    category.getName(), category.getDescription(), category.getId());
        }
        return category;
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM categories WHERE id = ?", id);
    }

    @Override
    public int countProducts(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE category_id = ?", Integer.class, id);
    }
}
