package co.edu.ucc.stockflow.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The app asks this {@link Clock} for "now" instead of calling {@code LocalDateTime.now()} directly,
 * so tests can freeze the time (for example on the last day of a month) with {@code Clock.fixed(...)}.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
