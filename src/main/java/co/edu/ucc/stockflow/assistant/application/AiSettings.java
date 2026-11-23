package co.edu.ucc.stockflow.assistant.application;

import co.edu.ucc.stockflow.settings.application.SettingsService;
import co.edu.ucc.stockflow.shared.util.Formats;
import org.springframework.stereotype.Service;

/**
 * Key and model for Gemini. The key can come from the GEMINI_API_KEY environment variable (it
 * wins, and is never saved) or be written in the app, which keeps it in the local database.
 * Free keys are created at https://aistudio.google.com/apikey.
 */
@Service
public class AiSettings {

    public static final String API_KEY = "ai.gemini.apiKey";
    public static final String MODEL = "ai.gemini.model";
    public static final String ENVIRONMENT_VARIABLE = "GEMINI_API_KEY";
    /** Stable, low-cost model made for reading documents (images and PDF) with structured output. */
    public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";

    private final SettingsService settings;

    public AiSettings(SettingsService settings) {
        this.settings = settings;
    }

    public String apiKey() {
        String fromEnvironment = System.getenv(ENVIRONMENT_VARIABLE);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) return fromEnvironment.trim();
        return settings.get(API_KEY, "").trim();
    }

    public boolean keyFromEnvironment() {
        String fromEnvironment = System.getenv(ENVIRONMENT_VARIABLE);
        return fromEnvironment != null && !fromEnvironment.isBlank();
    }

    public boolean hasKey() {
        return !apiKey().isEmpty();
    }

    public String model() {
        String model = settings.get(MODEL, DEFAULT_MODEL).trim();
        return model.isEmpty() ? DEFAULT_MODEL : model;
    }

    /** Saves the key (empty removes it) and the model (empty goes back to the default). */
    public void save(String apiKey, String model) {
        settings.set(API_KEY, apiKey == null ? "" : apiKey.trim());
        String clean = Formats.clean(model);
        settings.set(MODEL, clean == null ? DEFAULT_MODEL : clean);
    }

    /** "AIza…X9k2": enough to recognise the key without showing it. */
    public static String masked(String key) {
        if (key == null || key.length() < 10) return key == null || key.isEmpty() ? "" : "••••";
        return key.substring(0, 4) + "…" + key.substring(key.length() - 4);
    }
}
