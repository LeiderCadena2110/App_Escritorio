package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.Supplier;
import co.edu.ucc.stockflow.repository.SupplierRepository;
import co.edu.ucc.stockflow.util.Formats;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    public List<Supplier> search(String text) {
        return repository.search(text);
    }

    public List<Supplier> findActive() {
        return repository.findActive();
    }

    @Transactional
    public Supplier save(Supplier s) {
        s.setTaxId(Formats.clean(s.getTaxId()));
        s.setName(Formats.clean(s.getName()));
        s.setContactName(Formats.clean(s.getContactName()));
        s.setPhone(Formats.clean(s.getPhone()));
        s.setEmail(Formats.clean(s.getEmail()));
        s.setAddress(Formats.clean(s.getAddress()));
        if (s.getName() == null) throw new BusinessException("Escriba el nombre del proveedor.");
        return repository.save(s);
    }

    /**
     * Deletes the supplier. If products point to it, it is deactivated instead.
     *
     * @return true if it was deleted, false if it was deactivated
     */
    @Transactional
    public boolean delete(Supplier s) {
        if (repository.countProducts(s.getId()) > 0) {
            s.setActive(false);
            repository.save(s);
            return false;
        }
        repository.delete(s.getId());
        return true;
    }
}
