package co.edu.ucc.stockflow.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.ucc.stockflow.catalog.domain.Category;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;

/** Thanks to the repository port, the service is tested without Spring and without a database. */
class CategoryServiceTest {

    private final InMemoryCategoryRepository repository = new InMemoryCategoryRepository();
    private final CategoryService service = new CategoryService(repository);

    @Test
    void namesAreTrimmedAndEmptyDescriptionsBecomeNull() {
        Category saved = service.save(new Category(null, "  Deportivos  ", "   "));
        assertEquals("Deportivos", saved.getName());
        assertNull(saved.getDescription());
    }

    @Test
    void namesMustBeUniqueIgnoringCase() {
        service.save(new Category(null, "Deportivos", null));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.save(new Category(null, "deportivos", null)));
        assertEquals(ErrorCode.CATEGORY_DUPLICATED, e.getCode());
    }

    @Test
    void renamingACategoryToItsOwnNameIsAllowed() {
        Category c = service.save(new Category(null, "Casuales", null));
        service.save(new Category(c.getId(), "CASUALES", "Día a día"));
        assertEquals("CASUALES", repository.findById(c.getId()).orElseThrow().getName());
    }

    @Test
    void aNameIsRequired() {
        assertEquals(ErrorCode.CATEGORY_NAME_REQUIRED, assertThrows(BusinessException.class,
                () -> service.save(new Category(null, " ", null))).getCode());
    }
}
