package co.edu.ucc.stockflow.catalog.application;

import co.edu.ucc.stockflow.catalog.domain.Category;
import co.edu.ucc.stockflow.catalog.infrastructure.CategoryRepository;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import co.edu.ucc.stockflow.shared.util.Formats;
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
        if (name == null) throw new BusinessException(ErrorCode.CATEGORY_NAME_REQUIRED);
        repository.findByName(name)
                .filter(other -> !other.getId().equals(category.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(ErrorCode.CATEGORY_DUPLICATED, name);
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
