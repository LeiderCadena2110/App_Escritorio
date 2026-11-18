package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.dto.BusinessInfo;
import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.exception.ErrorCode;
import co.edu.ucc.stockflow.repository.SettingRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and saves the app settings (business data, taxes and appearance). Numbering lives in SequenceService. */
@Service
public class SettingsService {

    public static final String BUSINESS_NAME = "business.name";
    public static final String BUSINESS_TAX_ID = "business.taxId";
    public static final String BUSINESS_ADDRESS = "business.address";
    public static final String BUSINESS_PHONE = "business.phone";
    public static final String BUSINESS_EMAIL = "business.email";
    public static final String INVOICE_FOOTER = "invoice.footer";
    public static final String TAX_RATE = "invoice.taxRate";
    public static final String THEME = "ui.theme";
    public static final String LAST_SECTION = "ui.lastSection";
    public static final String WINDOW = "ui.window";
    public static final String SAMPLE_DATA_LOADED = "data.sampleLoaded";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("19");

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
            throw new BusinessException(ErrorCode.SETTINGS_BUSINESS_NAME_REQUIRED);
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
            throw new BusinessException(ErrorCode.SETTINGS_TAX_RATE_OUT_OF_RANGE);
        }
        set(TAX_RATE, rate.stripTrailingZeros().toPlainString());
    }

    public String theme() {
        return get(THEME, THEME_LIGHT);
    }
}
