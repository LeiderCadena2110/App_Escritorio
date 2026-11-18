package co.edu.ucc.stockflow.sales.application.payment;

import co.edu.ucc.stockflow.customers.domain.Customer;
import co.edu.ucc.stockflow.sales.domain.PaymentMethod;
import co.edu.ucc.stockflow.sales.domain.payment.PaymentPolicy;
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
