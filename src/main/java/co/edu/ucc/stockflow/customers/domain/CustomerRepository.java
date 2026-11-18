package co.edu.ucc.stockflow.customers.domain;

import java.util.List;
import java.util.Optional;

/**
 * Where customers are stored.
 *
 * <p>Port: the application layer depends on this interface and JdbcCustomerRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface CustomerRepository {

    List<Customer> search(String text);

    Optional<Customer> findById(long id);

    Optional<Customer> findByDocument(String documentNumber);

    Customer save(Customer c);

    void delete(long id);

    int countInvoices(long id);

    int count();
}
