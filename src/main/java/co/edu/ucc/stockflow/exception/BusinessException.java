package co.edu.ucc.stockflow.exception;

/**
 * A business rule was broken (duplicated SKU, not enough stock, empty invoice...).
 * The message is written for the user and is shown as is in a dialog.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
