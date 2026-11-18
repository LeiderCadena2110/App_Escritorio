package co.edu.ucc.stockflow.catalog.application;

import co.edu.ucc.stockflow.catalog.domain.Category;
import co.edu.ucc.stockflow.catalog.domain.CategoryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double of the CategoryRepository port: keeps categories in a map instead of a database. */
class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<Long, Category> rows = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public List<Category> findAll() {
        List<Category> all = new ArrayList<>(rows.values());
        all.sort(Comparator.comparing(Category::getName));
        return all;
    }

    @Override
    public Optional<Category> findById(long id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public Optional<Category> findByName(String name) {
        return rows.values().stream().filter(c -> c.getName().equalsIgnoreCase(name.trim())).findFirst();
    }

    @Override
    public Category save(Category category) {
        if (category.getId() == null) category.setId(nextId++);
        rows.put(category.getId(), category);
        return category;
    }

    @Override
    public void delete(long id) {
        rows.remove(id);
    }

    @Override
    public int countProducts(long id) {
        return 0;
    }
}
