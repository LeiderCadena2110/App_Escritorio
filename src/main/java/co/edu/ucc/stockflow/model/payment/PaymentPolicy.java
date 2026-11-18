package co.edu.ucc.stockflow.model.payment;

import co.edu.ucc.stockflow.customers.domain.Customer;
import co.edu.ucc.stockflow.model.PaymentMethod;
import java.math.BigDecimal;

/**
 * Strategy with the rules of one payment method. Adding a method (for example Nequi or mixed
 * payments) means adding one implementation; the invoice service does not change.
 */
public interface PaymentPolicy {

    PaymentMethod method();

    /**
     * Checks the payment and returns the amount that is recorded as paid.
     *
     * @param total    total of the invoice
     * @param received money the customer gave (only meaningful for cash; 0 if not typed)
     * @param customer customer of the invoice
     * @throws co.edu.ucc.stockflow.shared.domain.BusinessException when the payment is not valid
     */
    BigDecimal settle(BigDecimal total, BigDecimal received, Customer customer);
}
