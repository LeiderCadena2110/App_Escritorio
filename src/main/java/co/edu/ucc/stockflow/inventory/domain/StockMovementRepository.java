package co.edu.ucc.stockflow.inventory.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stock history (kardex): every movement of every product.
 *
 * <p>Port: the application layer depends on this interface and JdbcStockMovementRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface StockMovementRepository {

    StockMovement insert(StockMovement m);

    /** Movements between two dates (inclusive start, exclusive end), newest first. Null filters are skipped. */
    List<StockMovement> search(LocalDateTime from, LocalDateTime to, Long productId, MovementType type);

    List<StockMovement> findByProduct(long productId);
}
