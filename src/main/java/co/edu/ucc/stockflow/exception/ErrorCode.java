package co.edu.ucc.stockflow.exception;

/**
 * Every business rule that can fail has a code. The code is what the logic knows; the text the
 * user reads lives in {@code i18n/messages.properties} under the key {@code error.<CODE>}, so the
 * rules do not depend on the language of the screen.
 */
public enum ErrorCode {
    // Products
    PRODUCT_SKU_REQUIRED,
    PRODUCT_SKU_TOO_LONG,
    PRODUCT_NAME_REQUIRED,
    PRODUCT_BARCODE_INVALID,
    PRODUCT_COST_NEGATIVE,
    PRODUCT_PRICE_NOT_POSITIVE,
    PRODUCT_MIN_STOCK_NEGATIVE,
    PRODUCT_TAX_RATE_OUT_OF_RANGE,
    PRODUCT_INITIAL_STOCK_NEGATIVE,
    PRODUCT_SKU_DUPLICATED,
    PRODUCT_BARCODE_DUPLICATED,
    PRODUCT_INACTIVE,
    PRODUCT_NOT_SELECTED,
    PRODUCT_NOT_FOUND,

    // Stock
    STOCK_INSUFFICIENT,
    STOCK_SOLD_OUT,
    STOCK_ADJUSTMENT_ZERO,
    STOCK_QUANTITY_NOT_POSITIVE,

    // Customers, suppliers and categories
    CUSTOMER_DOCUMENT_REQUIRED,
    CUSTOMER_NAME_REQUIRED,
    CUSTOMER_EMAIL_INVALID,
    CUSTOMER_DOCUMENT_DUPLICATED,
    CUSTOMER_FINAL_CONSUMER_PROTECTED,
    CUSTOMER_HAS_INVOICES,
    CUSTOMER_NOT_FOUND,
    SUPPLIER_NAME_REQUIRED,
    CATEGORY_NAME_REQUIRED,
    CATEGORY_DUPLICATED,

    // Invoices
    INVOICE_EMPTY,
    INVOICE_DISCOUNT_NEGATIVE,
    INVOICE_DISCOUNT_ABOVE_SUBTOTAL,
    INVOICE_QUANTITY_NOT_POSITIVE,
    INVOICE_CASH_INSUFFICIENT,
    INVOICE_ALREADY_CANCELLED,
    INVOICE_CANCEL_REASON_REQUIRED,
    INVOICE_NOT_FOUND,
    PAYMENT_CREDIT_REQUIRES_CUSTOMER,

    // Settings and numbering
    SEQUENCE_PREFIX_TOO_LONG,
    SEQUENCE_NEXT_NOT_POSITIVE,
    SETTINGS_BUSINESS_NAME_REQUIRED,
    SETTINGS_TAX_RATE_OUT_OF_RANGE;

    /** Key of the message in i18n/messages.properties. */
    public String messageKey() {
        return "error." + name();
    }
}
