package co.edu.ucc.stockflow.exception;

import java.util.Arrays;

/**
 * A business rule was broken (duplicated SKU, not enough stock, empty invoice...).
 * It carries an {@link ErrorCode} for the logic and tests, and a ready-to-show message for the user.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final transient Object[] args;

    public BusinessException(ErrorCode code, Object... args) {
        super(Messages.format(code, args));
        this.code = code;
        this.args = args.clone();
    }

    public ErrorCode getCode() {
        return code;
    }

    public Object[] getArgs() {
        return Arrays.copyOf(args, args.length);
    }
}
