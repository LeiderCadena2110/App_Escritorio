package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.model.Category;
import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.Supplier;
import co.edu.ucc.stockflow.repository.ProductRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prepares the database on start: the "final consumer" customer always, and a small sample
 * catalog (a shoe store) the first time, so the app can be tried right away.
 */
@Service
public class DataSeeder {

    private final SettingsService settings;
    private final CustomerService customers;
    private final CategoryService categories;
    private final SupplierService suppliers;
    private final ProductService products;
    private final ProductRepository productRepository;

    public DataSeeder(SettingsService settings, CustomerService customers, CategoryService categories,
                      SupplierService suppliers, ProductService products, ProductRepository productRepository) {
        this.settings = settings;
        this.customers = customers;
        this.categories = categories;
        this.suppliers = suppliers;
        this.products = products;
        this.productRepository = productRepository;
    }

    @Transactional
    public void prepare() {
        customers.finalConsumer();
        boolean alreadyLoaded = Boolean.parseBoolean(settings.get(SettingsService.SAMPLE_DATA_LOADED, "false"));
        if (!alreadyLoaded && productRepository.countActive() == 0) {
            loadSampleData();
        }
        settings.set(SettingsService.SAMPLE_DATA_LOADED, "true");
    }

    @Transactional
    public void loadSampleData() {
        Category sports = category("Deportivos", "Tenis para correr, fútbol y entrenamiento");
        Category casual = category("Casuales", "Calzado para el día a día");
        Category formal = category("Formales", "Zapatos de vestir");
        Category sandals = category("Sandalias", "Sandalias y chanclas");
        Category accessories = category("Accesorios", "Medias, plantillas y productos de limpieza");

        Supplier calzado = supplier("900123456-1", "Calzado Andino S.A.S.", "Marta Rosero", "602 7311234");
        Supplier deportes = supplier("901987654-3", "Distribuidora Deportiva del Sur", "Carlos Muñoz", "310 4567890");
        Supplier insumos = supplier("800555222-7", "Insumos y Cuidado Ltda.", "Laura Pérez", "315 2223344");

        product("DEP-001", "Tenis running Ultra 3", sports, deportes, "145000", "229900", 18, 5);
        product("DEP-002", "Guayos fútbol Pro FG", sports, deportes, "120000", "189900", 9, 4);
        product("DEP-003", "Tenis training Flex", sports, deportes, "98000", "159900", 3, 4);
        product("CAS-001", "Tenis urbanos lona blanca", casual, calzado, "55000", "99900", 25, 8);
        product("CAS-002", "Mocasín casual café", casual, calzado, "70000", "129900", 12, 4);
        product("CAS-003", "Botín casual cuero negro", casual, calzado, "110000", "199900", 2, 3);
        product("FOR-001", "Zapato Oxford negro", formal, calzado, "95000", "174900", 10, 3);
        product("FOR-002", "Zapato de tacón 7 cm", formal, calzado, "85000", "159900", 7, 3);
        product("SAN-001", "Sandalia playera unisex", sandals, calzado, "18000", "34900", 40, 10);
        product("SAN-002", "Sandalia cuero dama", sandals, calzado, "45000", "84900", 4, 5);
        product("ACC-001", "Medias deportivas x3", accessories, insumos, "9000", "18900", 60, 15);
        product("ACC-002", "Kit limpieza de calzado", accessories, insumos, "15000", "29900", 14, 5);
        product("ACC-003", "Plantillas de gel", accessories, insumos, "12000", "24900", 0, 5);

        customer("CC", "1085123456", "Ana María Benavides", "3001234567", "ana.benavides@correo.com");
        customer("CC", "1086987654", "Juan Pablo Ortiz", "3109876543", null);
        customer("NIT", "900777888-2", "Colegio San Felipe", "6027201010", "compras@sanfelipe.edu.co");
    }

    private Category category(String name, String description) {
        return categories.save(new Category(null, name, description));
    }

    private Supplier supplier(String taxId, String name, String contact, String phone) {
        Supplier s = new Supplier();
        s.setTaxId(taxId);
        s.setName(name);
        s.setContactName(contact);
        s.setPhone(phone);
        return suppliers.save(s);
    }

    private void product(String sku, String name, Category category, Supplier supplier, String cost, String price,
                         int stock, int minStock) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategoryId(category.getId());
        p.setSupplierId(supplier.getId());
        p.setCostPrice(new BigDecimal(cost));
        p.setSalePrice(new BigDecimal(price));
        p.setStock(stock);
        p.setMinStock(minStock);
        p.setUnit(category.getName().equals("Accesorios") ? "UND" : "PAR");
        products.create(p);
    }

    private void customer(String type, String document, String name, String phone, String email) {
        Customer c = new Customer();
        c.setDocumentType(type);
        c.setDocumentNumber(document);
        c.setName(name);
        c.setPhone(phone);
        c.setEmail(email);
        customers.save(c);
    }
}
