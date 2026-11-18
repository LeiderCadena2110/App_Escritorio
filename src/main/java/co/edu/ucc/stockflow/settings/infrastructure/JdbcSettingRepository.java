package co.edu.ucc.stockflow.settings.infrastructure;

import co.edu.ucc.stockflow.settings.domain.SettingRepository;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Key/value settings (business data, tax rate, theme...). */
@Repository
public class JdbcSettingRepository implements SettingRepository {

    private final JdbcTemplate jdbc;

    public JdbcSettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> find(String key) {
        return jdbc.query("SELECT setting_value FROM settings WHERE setting_key = ?",
                (rs, n) -> rs.getString(1), key).stream().findFirst();
    }

    @Override
    public void save(String key, String value) {
        jdbc.update("MERGE INTO settings (setting_key, setting_value) KEY (setting_key) VALUES (?, ?)", key, value);
    }
}
