package co.edu.ucc.stockflow.shared.ui;

import java.util.Optional;

/**
 * A screen that can hold work that is not saved yet (for example a sale that was not charged).
 * Before closing the app the main window asks every screen, so nothing is lost by accident.
 */
public interface PendingWork {

    /** Why closing now would lose work ("Hay una venta sin cobrar…"), or empty if nothing would be lost. */
    Optional<String> pendingWork();
}
