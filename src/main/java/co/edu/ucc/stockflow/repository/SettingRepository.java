package co.edu.ucc.stockflow.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Key/value settings (business data, tax rate, theme...). */
@Repository
public class SettingRepository {

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> find(String key) {
        return jdbc.query("SELECT setting_value FROM settings WHERE setting_key = ?",
                (rs, n) -> rs.getString(1), key).stream().findFirst();
    }

    public void save(String key, String value) {
        jdbc.update("MERGE INTO settings (setting_key, setting_value) KEY (setting_key) VALUES (?, ?)", key, value);
    }
}
