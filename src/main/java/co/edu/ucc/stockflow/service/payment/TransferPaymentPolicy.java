package co.edu.ucc.stockflow.service.payment;

import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.payment.PaymentPolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Transferencia (Nequi, Daviplata, banco): se paga el total exacto. */
@Component
public class TransferPaymentPolicy implements PaymentPolicy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.TRANSFER;
    }

    @Override
    public BigDecimal settle(BigDecimal total, BigDecimal received, Customer customer) {
        return total;
    }
}
