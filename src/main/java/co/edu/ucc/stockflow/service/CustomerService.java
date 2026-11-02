package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.repository.CustomerRepository;
import co.edu.ucc.stockflow.util.Formats;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    /** Document types used in Colombia. */
    public static final List<String> DOCUMENT_TYPES = List.of("CC", "NIT", "CE", "TI", "PP");

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public List<Customer> search(String text) {
        return repository.search(text);
    }

    /** Generic customer used when the buyer does not give their data. */
    @Transactional
    public Customer finalConsumer() {
        return repository.findByDocument(Customer.FINAL_CONSUMER_DOCUMENT).orElseGet(() -> {
            Customer c = new Customer();
            c.setDocumentType("CC");
            c.setDocumentNumber(Customer.FINAL_CONSUMER_DOCUMENT);
            c.setName("Consumidor final");
            return repository.save(c);
        });
    }

    @Transactional
    public Customer save(Customer c) {
        c.setDocumentNumber(Formats.clean(c.getDocumentNumber()));
        c.setName(Formats.clean(c.getName()));
        c.setPhone(Formats.clean(c.getPhone()));
        c.setEmail(Formats.clean(c.getEmail()));
        c.setAddress(Formats.clean(c.getAddress()));
        if (c.getDocumentType() == null || !DOCUMENT_TYPES.contains(c.getDocumentType())) c.setDocumentType("CC");
        if (c.getDocumentNumber() == null) throw new BusinessException("Escriba el número de documento del cliente.");
        if (c.getName() == null) throw new BusinessException("Escriba el nombre del cliente.");
        if (c.getEmail() != null && !EMAIL.matcher(c.getEmail()).matches()) {
            throw new BusinessException("El correo \"" + c.getEmail() + "\" no es válido.");
        }
        repository.findByDocument(c.getDocumentNumber())
                .filter(other -> !other.getId().equals(c.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Ya existe un cliente con el documento " + c.getDocumentNumber()
                            + " (" + other.getName() + ").");
                });
        return repository.save(c);
    }

    /** Customers with invoices cannot be deleted (the invoices point to them). */
    @Transactional
    public void delete(Customer c) {
        if (c.isFinalConsumer()) throw new BusinessException("El cliente \"Consumidor final\" no se puede eliminar.");
        int invoices = repository.countInvoices(c.getId());
        if (invoices > 0) {
            throw new BusinessException("No se puede eliminar a " + c.getName() + " porque tiene " + invoices
                    + " factura(s).");
        }
        repository.delete(c.getId());
    }
}
