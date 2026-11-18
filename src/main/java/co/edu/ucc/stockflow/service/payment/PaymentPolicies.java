package co.edu.ucc.stockflow.service.payment;

import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.payment.PaymentPolicy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Finds the policy of each payment method among the {@link PaymentPolicy} beans Spring created. */
@Component
public class PaymentPolicies {

    private final Map<PaymentMethod, PaymentPolicy> byMethod = new EnumMap<>(PaymentMethod.class);

    public PaymentPolicies(List<PaymentPolicy> policies) {
        for (PaymentPolicy p : policies) {
            PaymentPolicy previous = byMethod.put(p.method(), p);
            if (previous != null) {
                throw new IllegalStateException("Two policies for " + p.method() + ": " + previous + ", " + p);
            }
        }
    }

    public PaymentPolicy forMethod(PaymentMethod method) {
        PaymentPolicy policy = byMethod.get(method);
        if (policy == null) throw new IllegalStateException("No payment policy for " + method);
        return policy;
    }
}
