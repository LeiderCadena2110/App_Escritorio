package co.edu.ucc.stockflow.exception;

/** The record no longer exists (for example, it was deleted in another window). */
public class NotFoundException extends BusinessException {

    public NotFoundException(ErrorCode code, Object id) {
        super(code, id);
    }
}
