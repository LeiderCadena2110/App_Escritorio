package co.edu.ucc.stockflow.exception;

/** The record no longer exists (for example, it was deleted in another window). */
public class NotFoundException extends BusinessException {

    public NotFoundException(String what, Object id) {
        super("No se encontró " + what + " con id " + id + ".");
    }
}
