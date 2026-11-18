package co.edu.ucc.stockflow.settings.infrastructure;

import co.edu.ucc.stockflow.settings.domain.SequenceRepository;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Numbering series (table sequences): prefix and next number of each type of document. */
@Repository
public class JdbcSequenceRepository implements SequenceRepository {


    private final JdbcTemplate jdbc;

    public JdbcSequenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Series> find(String name) {
        return jdbc.query("SELECT name, prefix, next_value FROM sequences WHERE name = ?",
                (rs, n) -> new Series(rs.getString("name"), rs.getString("prefix"), rs.getLong("next_value")),
                name).stream().findFirst();
    }

    /** Reads the series and locks its row until the transaction ends, so two sales never get the same number. */
    @Override
    public Optional<Series> findForUpdate(String name) {
        return jdbc.query("SELECT name, prefix, next_value FROM sequences WHERE name = ? FOR UPDATE",
                (rs, n) -> new Series(rs.getString("name"), rs.getString("prefix"), rs.getLong("next_value")),
                name).stream().findFirst();
    }

    @Override
    public void save(Series series) {
        jdbc.update("MERGE INTO sequences (name, prefix, next_value) KEY (name) VALUES (?, ?, ?)",
                series.name(), series.prefix(), series.nextValue());
    }

    @Override
    public void setNextValue(String name, long nextValue) {
        jdbc.update("UPDATE sequences SET next_value = ? WHERE name = ?", nextValue, name);
    }
}
