package co.edu.ucc.stockflow.service.payment;

import co.edu.ucc.stockflow.customers.domain.Customer;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.payment.PaymentPolicy;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import co.edu.ucc.stockflow.shared.util.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Cash: the money received must cover the total; if nothing was typed, the exact amount is assumed. */
@Component
public class CashPaymentPolicy implements PaymentPolicy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH;
    }

    @Override
    public BigDecimal settle(BigDecimal total, BigDecimal received, Customer customer) {
        BigDecimal paid = received == null ? BigDecimal.ZERO : Money.round(received);
        if (paid.signum() == 0) return total;
        if (paid.compareTo(total) < 0) {
            throw new BusinessException(ErrorCode.INVOICE_CASH_INSUFFICIENT, Money.format(paid), Money.format(total));
        }
        return paid;
    }
}
