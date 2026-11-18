package co.edu.ucc.stockflow.settings.domain;

import java.util.Optional;

/**
 * Numbering series (prefix and next number of each type of document).
 *
 * <p>Port: the application layer depends on this interface and JdbcSequenceRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface SequenceRepository {

    /** Prefix and next number of a series. */
    record Series(String name, String prefix, long nextValue) {
    }

    Optional<Series> find(String name);

    /** Reads the series and locks its row until the transaction ends, so two sales never get the same number. */
    Optional<Series> findForUpdate(String name);

    void save(Series series);

    void setNextValue(String name, long nextValue);
}
