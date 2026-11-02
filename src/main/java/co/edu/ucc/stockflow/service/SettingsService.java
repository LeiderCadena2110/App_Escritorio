package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.dto.BusinessInfo;
import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.repository.SettingRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and saves the app settings (business data, taxes, invoice numbering and appearance). */
@Service
public class SettingsService {

    public static final String BUSINESS_NAME = "business.name";
    public static final String BUSINESS_TAX_ID = "business.taxId";
    public static final String BUSINESS_ADDRESS = "business.address";
    public static final String BUSINESS_PHONE = "business.phone";
    public static final String BUSINESS_EMAIL = "business.email";
    public static final String INVOICE_FOOTER = "invoice.footer";
    public static final String TAX_RATE = "invoice.taxRate";
    public static final String INVOICE_PREFIX = "invoice.prefix";
    public static final String INVOICE_NEXT_NUMBER = "invoice.nextNumber";
    public static final String THEME = "ui.theme";
    public static final String LAST_SECTION = "ui.lastSection";
    public static final String WINDOW = "ui.window";
    public static final String SAMPLE_DATA_LOADED = "data.sampleLoaded";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("19");
    public static final String DEFAULT_PREFIX = "FV-";

    private final SettingRepository repository;

    public SettingsService(SettingRepository repository) {
        this.repository = repository;
    }

    public String get(String key, String defaultValue) {
        return repository.find(key).filter(v -> !v.isBlank()).orElse(defaultValue);
    }

    public void set(String key, String value) {
        repository.save(key, value == null ? "" : value.trim());
    }

    public BusinessInfo businessInfo() {
        return new BusinessInfo(
                get(BUSINESS_NAME, "Mi Negocio"),
                get(BUSINESS_TAX_ID, ""),
                get(BUSINESS_ADDRESS, ""),
                get(BUSINESS_PHONE, ""),
                get(BUSINESS_EMAIL, ""),
                get(INVOICE_FOOTER, "¡Gracias por su compra!"));
    }

    @Transactional
    public void saveBusinessInfo(BusinessInfo info) {
        if (info.name() == null || info.name().isBlank()) {
            throw new BusinessException("Escriba el nombre del negocio.");
        }
        set(BUSINESS_NAME, info.name());
        set(BUSINESS_TAX_ID, info.taxId());
        set(BUSINESS_ADDRESS, info.address());
        set(BUSINESS_PHONE, info.phone());
        set(BUSINESS_EMAIL, info.email());
        set(INVOICE_FOOTER, info.invoiceFooter());
    }

    /** VAT percentage applied to new invoices (19 by default). */
    public BigDecimal taxRate() {
        try {
            return new BigDecimal(get(TAX_RATE, DEFAULT_TAX_RATE.toPlainString()));
        } catch (NumberFormatException e) {
            return DEFAULT_TAX_RATE;
        }
    }

    public void setTaxRate(BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("El IVA debe estar entre 0 y 100 %.");
        }
        set(TAX_RATE, rate.stripTrailingZeros().toPlainString());
    }

    public String invoicePrefix() {
        return get(INVOICE_PREFIX, DEFAULT_PREFIX);
    }

    public void setInvoicePrefix(String prefix) {
        if (prefix != null && prefix.trim().length() > 10) {
            throw new BusinessException("El prefijo de factura puede tener máximo 10 caracteres.");
        }
        set(INVOICE_PREFIX, prefix);
    }

    public long nextInvoiceNumber() {
        try {
            return Long.parseLong(get(INVOICE_NEXT_NUMBER, "1"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public void setNextInvoiceNumber(long next) {
        if (next < 1) throw new BusinessException("El siguiente número de factura debe ser mayor que 0.");
        set(INVOICE_NEXT_NUMBER, Long.toString(next));
    }

    /** Builds the number shown on the invoice: prefix + 6 digits, for example FV-000123. */
    public static String formatInvoiceNumber(String prefix, long number) {
        return (prefix == null ? "" : prefix.trim()) + String.format("%06d", number);
    }

    public String theme() {
        return get(THEME, THEME_LIGHT);
    }
}
