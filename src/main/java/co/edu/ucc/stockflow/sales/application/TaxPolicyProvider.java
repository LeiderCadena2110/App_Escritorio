package co.edu.ucc.stockflow.sales.application;

import co.edu.ucc.stockflow.sales.domain.tax.TaxPolicy;
import co.edu.ucc.stockflow.settings.application.SettingsService;
import org.springframework.stereotype.Component;

/** Chooses the VAT rule of the business from its settings (sales asks; settings only stores the choice). */
@Component
public class TaxPolicyProvider {

    private final SettingsService settings;

    public TaxPolicyProvider(SettingsService settings) {
        this.settings = settings;
    }

    public TaxPolicy current() {
        return TaxPolicy.forRegime(settings.isVatResponsible());
    }
}
