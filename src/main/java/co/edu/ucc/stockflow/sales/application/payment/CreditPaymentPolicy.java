package co.edu.ucc.stockflow.sales.application.payment;

import co.edu.ucc.stockflow.customers.domain.Customer;
import co.edu.ucc.stockflow.sales.domain.PaymentMethod;
import co.edu.ucc.stockflow.sales.domain.payment.PaymentPolicy;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Credit ("fiado"): nothing is paid now, so the business must know who owes the money.
 * It cannot be used with the generic "Consumidor final" customer.
 */
@Component
public class CreditPaymentPolicy implements PaymentPolicy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CREDIT;
    }

    @Override
    public BigDecimal settle(BigDecimal total, BigDecimal received, Customer customer) {
        if (customer == null || customer.isFinalConsumer()) {
            throw new BusinessException(ErrorCode.PAYMENT_CREDIT_REQUIRES_CUSTOMER);
        }
        return BigDecimal.ZERO;
    }
}
