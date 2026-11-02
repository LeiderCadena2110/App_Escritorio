package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.Category;
import co.edu.ucc.stockflow.repository.CategoryRepository;
import co.edu.ucc.stockflow.util.Formats;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Category save(Category category) {
        String name = Formats.clean(category.getName());
        if (name == null) throw new BusinessException("Escriba el nombre de la categoría.");
        repository.findByName(name)
                .filter(other -> !other.getId().equals(category.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Ya existe una categoría llamada \"" + name + "\".");
                });
        category.setName(name);
        category.setDescription(Formats.clean(category.getDescription()));
        return repository.save(category);
    }

    /** Deletes the category; its products stay without a category. */
    @Transactional
    public void delete(Category category) {
        repository.delete(category.getId());
    }

    public int countProducts(Category category) {
        return repository.countProducts(category.getId());
    }
}
