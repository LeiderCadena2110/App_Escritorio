package co.edu.ucc.stockflow.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * Where product categories are stored.
 *
 * <p>Port: the application layer depends on this interface and JdbcCategoryRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(long id);

    Optional<Category> findByName(String name);

    Category save(Category category);

    void delete(long id);

    int countProducts(long id);
}
