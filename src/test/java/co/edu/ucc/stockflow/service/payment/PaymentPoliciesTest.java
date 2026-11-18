package co.edu.ucc.stockflow.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.payment.PaymentPolicy;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentPoliciesTest {

    private static final BigDecimal TOTAL = new BigDecimal("22491.00");
    private final PaymentPolicies policies = new PaymentPolicies(List.of(new CashPaymentPolicy(),
            new CardPaymentPolicy(), new TransferPaymentPolicy(), new CreditPaymentPolicy()));

    private static Customer customer(String document) {
        Customer c = new Customer();
        c.setDocumentNumber(document);
        c.setName("Cliente");
        return c;
    }

    @Test
    void everyPaymentMethodHasAPolicy() {
        for (PaymentMethod m : PaymentMethod.values()) assertEquals(m, policies.forMethod(m).method());
        assertInstanceOf(CreditPaymentPolicy.class, policies.forMethod(PaymentMethod.CREDIT));
    }

    @Test
    void cashMustCoverTheTotalOrIsAssumedExact() {
        PaymentPolicyAssert cash = new PaymentPolicyAssert(policies.forMethod(PaymentMethod.CASH));
        assertEquals(new BigDecimal("50000.00"), cash.settle("50000"));
        assertEquals(TOTAL, cash.settle("0"));
        assertEquals(ErrorCode.INVOICE_CASH_INSUFFICIENT,
                assertThrows(BusinessException.class, () -> cash.settle("20000")).getCode());
    }

    @Test
    void cardAndTransferPayTheExactTotal() {
        assertEquals(TOTAL, policies.forMethod(PaymentMethod.CARD).settle(TOTAL, BigDecimal.ONE, null));
        assertEquals(TOTAL, policies.forMethod(PaymentMethod.TRANSFER).settle(TOTAL, null, null));
    }

    @Test
    void creditNeedsAnIdentifiedCustomerAndPaysNothingNow() {
        var credit = policies.forMethod(PaymentMethod.CREDIT);
        assertEquals(BigDecimal.ZERO, credit.settle(TOTAL, null, customer("1085123456")));
        assertEquals(ErrorCode.PAYMENT_CREDIT_REQUIRES_CUSTOMER, assertThrows(BusinessException.class,
                () -> credit.settle(TOTAL, null, customer(Customer.FINAL_CONSUMER_DOCUMENT))).getCode());
        assertThrows(BusinessException.class, () -> credit.settle(TOTAL, null, null));
    }

    @Test
    void twoPoliciesForTheSameMethodAreAConfigurationError() {
        assertThrows(IllegalStateException.class,
                () -> new PaymentPolicies(List.of(new CashPaymentPolicy(), new CashPaymentPolicy())));
    }

    /** Small helper to call the cash policy with text amounts. */
    private record PaymentPolicyAssert(co.edu.ucc.stockflow.model.payment.PaymentPolicy policy) {
        BigDecimal settle(String received) {
            return policy.settle(TOTAL, new BigDecimal(received), null);
        }
    }
}
