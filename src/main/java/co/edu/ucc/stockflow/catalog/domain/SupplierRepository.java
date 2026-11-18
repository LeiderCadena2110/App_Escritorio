package co.edu.ucc.stockflow.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * Where suppliers are stored.
 *
 * <p>Port: the application layer depends on this interface and JdbcSupplierRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface SupplierRepository {

    List<Supplier> search(String text);

    List<Supplier> findActive();

    Optional<Supplier> findById(long id);

    Supplier save(Supplier s);

    void delete(long id);

    int countProducts(long id);
}
