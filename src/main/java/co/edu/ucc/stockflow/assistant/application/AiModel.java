package co.edu.ucc.stockflow.assistant.application;

import java.util.List;

/**
 * Port to a generative AI model. The assistant only knows this interface; the Gemini adapter in
 * infrastructure implements it, and the tests use a fake that answers with fixed text, so no test
 * needs internet or a key.
 *
 * <p>Failures are {@link co.edu.ucc.stockflow.shared.domain.BusinessException}s with an AI_* code
 * (invalid key, quota, no connection...), so the screen shows a clear message.</p>
 */
public interface AiModel {

    /** True when there is a key to call the model. */
    boolean isConfigured();

    /** Provider and model, for the screen ("Gemini · gemini-3.5-flash-lite"). */
    String description();

    /**
     * Asks for an answer that follows a JSON Schema (structured output), optionally reading files.
     *
     * @return the JSON text produced by the model
     */
    String generateJson(String prompt, List<Attachment> attachments, String jsonSchema);

    /** Asks for a free text answer. */
    String generateText(String prompt);
}
