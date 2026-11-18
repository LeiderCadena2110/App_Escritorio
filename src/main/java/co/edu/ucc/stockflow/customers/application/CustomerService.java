package co.edu.ucc.stockflow.customers.application;

import co.edu.ucc.stockflow.customers.domain.Customer;
import co.edu.ucc.stockflow.customers.domain.CustomerRepository;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import co.edu.ucc.stockflow.shared.domain.NotFoundException;
import co.edu.ucc.stockflow.shared.util.Formats;
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

    public Customer findById(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, id));
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
        if (c.getDocumentNumber() == null) throw new BusinessException(ErrorCode.CUSTOMER_DOCUMENT_REQUIRED);
        if (c.getName() == null) throw new BusinessException(ErrorCode.CUSTOMER_NAME_REQUIRED);
        if (c.getEmail() != null && !EMAIL.matcher(c.getEmail()).matches()) {
            throw new BusinessException(ErrorCode.CUSTOMER_EMAIL_INVALID, c.getEmail());
        }
        repository.findByDocument(c.getDocumentNumber())
                .filter(other -> !other.getId().equals(c.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(ErrorCode.CUSTOMER_DOCUMENT_DUPLICATED,
                            c.getDocumentNumber(), other.getName());
                });
        return repository.save(c);
    }

    /** Customers with invoices cannot be deleted (the invoices point to them). */
    @Transactional
    public void delete(Customer c) {
        if (c.isFinalConsumer()) throw new BusinessException(ErrorCode.CUSTOMER_FINAL_CONSUMER_PROTECTED);
        int invoices = repository.countInvoices(c.getId());
        if (invoices > 0) {
            throw new BusinessException(ErrorCode.CUSTOMER_HAS_INVOICES, c.getName(), invoices);
        }
        repository.delete(c.getId());
    }
}
