package co.edu.ucc.stockflow.settings.domain;

import java.util.Optional;

/**
 * Key/value settings (business data, VAT regime, theme...).
 *
 * <p>Port: the application layer depends on this interface and JdbcSettingRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface SettingRepository {

    Optional<String> find(String key);

    void save(String key, String value);
}
