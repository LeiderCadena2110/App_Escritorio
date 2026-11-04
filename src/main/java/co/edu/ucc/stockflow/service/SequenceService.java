package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.repository.SequenceRepository;
import co.edu.ucc.stockflow.repository.SequenceRepository.Series;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consecutive numbers for documents (FV-000001, FV-000002...). */
@Service
public class SequenceService {

    public static final String INVOICE = "INVOICE";
    public static final String DEFAULT_INVOICE_PREFIX = "FV-";

    private final SequenceRepository repository;

    public SequenceService(SequenceRepository repository) {
        this.repository = repository;
    }

    /** Builds the number shown on the document: prefix + 6 digits, for example FV-000123. */
    public static String format(String prefix, long number) {
        return (prefix == null ? "" : prefix.trim()) + String.format("%06d", number);
    }

    public Series invoiceSeries() {
        return repository.find(INVOICE).orElse(new Series(INVOICE, DEFAULT_INVOICE_PREFIX, 1));
    }

    /** Number the next invoice will get (only to show it; it is not reserved). */
    public String peekInvoiceNumber() {
        Series s = invoiceSeries();
        return format(s.prefix(), s.nextValue());
    }

    @Transactional
    public void configureInvoices(String prefix, long nextValue) {
        String p = prefix == null ? "" : prefix.trim();
        if (p.length() > 10) throw new BusinessException("El prefijo de factura puede tener máximo 10 caracteres.");
        if (nextValue < 1) throw new BusinessException("El siguiente número de factura debe ser mayor que 0.");
        repository.save(new Series(INVOICE, p, nextValue));
    }

    /**
     * Takes the next invoice number and moves the counter forward. Numbers that already exist
     * (for example after changing the counter by hand) are skipped.
     */
    @Transactional
    public String reserveInvoiceNumber(Predicate<String> alreadyUsed) {
        Series s = repository.findForUpdate(INVOICE).orElseGet(() -> {
            Series created = new Series(INVOICE, DEFAULT_INVOICE_PREFIX, 1);
            repository.save(created);
            return created;
        });
        long next = s.nextValue();
        String number = format(s.prefix(), next);
        while (alreadyUsed.test(number)) {
            next++;
            number = format(s.prefix(), next);
        }
        repository.setNextValue(INVOICE, next + 1);
        return number;
    }
}
