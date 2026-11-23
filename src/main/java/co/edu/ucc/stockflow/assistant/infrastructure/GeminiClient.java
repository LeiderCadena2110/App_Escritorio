package co.edu.ucc.stockflow.assistant.infrastructure;

import co.edu.ucc.stockflow.assistant.application.AiModel;
import co.edu.ucc.stockflow.assistant.application.AiSettings;
import co.edu.ucc.stockflow.assistant.application.Attachment;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapter of {@link AiModel} for Google Gemini, using its REST API ({@code models/{model}:generateContent})
 * with the JDK HTTP client, so no SDK is needed.
 *
 * <p>The key goes in the {@code x-goog-api-key} header (never in the URL, so it does not end up in
 * logs). Structured output uses {@code responseMimeType = application/json} with a JSON Schema,
 * and files travel inside the request as base64 ({@code inlineData}).</p>
 */
@Component
public class GeminiClient implements AiModel {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private final AiSettings settings;
    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    public GeminiClient(AiSettings settings,
                        @Value("${stockflow.ai.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this(settings, baseUrl, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    /** For tests: another server and HTTP client. */
    GeminiClient(AiSettings settings, String baseUrl, HttpClient http) {
        this.settings = settings;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = http;
    }

    @Override
    public boolean isConfigured() {
        return settings.hasKey();
    }

    @Override
    public String description() {
        return "Gemini · " + settings.model();
    }

    @Override
    public String generateJson(String prompt, List<Attachment> attachments, String jsonSchema) {
        ObjectNode body = request(prompt, attachments);
        ObjectNode config = body.putObject("generationConfig");
        config.put("responseMimeType", "application/json");
        config.set("responseJsonSchema", parse(jsonSchema));
        config.put("temperature", 0); // extraction: always the same reading
        return call(body);
    }

    @Override
    public String generateText(String prompt) {
        ObjectNode body = request(prompt, List.of());
        body.putObject("generationConfig").put("temperature", 0.3);
        return call(body);
    }

    // ------------------------------------------------------------------ request

    private ObjectNode request(String prompt, List<Attachment> attachments) {
        ObjectNode body = json.createObjectNode();
        ArrayNode parts = body.putArray("contents").addObject().put("role", "user").putArray("parts");
        for (Attachment a : attachments) {
            parts.addObject().putObject("inlineData")
                    .put("mimeType", a.mimeType())
                    .put("data", Base64.getEncoder().encodeToString(a.data()));
        }
        parts.addObject().put("text", prompt);
        return body;
    }

    private String call(ObjectNode body) {
        String key = settings.apiKey();
        if (key.isEmpty()) throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        String model = settings.model();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/models/"
                        + URLEncoder.encode(model, StandardCharsets.UTF_8) + ":generateContent"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", key)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new BusinessException(ErrorCode.AI_TIMEOUT);
        } catch (IOException e) {
            log.warn("Gemini sin conexión: {}", e.toString());
            throw new BusinessException(ErrorCode.AI_NETWORK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_TIMEOUT);
        }
        if (response.statusCode() != 200) throw failure(response.statusCode(), response.body(), model);
        return text(response.body());
    }

    // ------------------------------------------------------------------ response

    /** Joins the text parts of the first candidate (skipping the model's "thoughts", if any). */
    String text(String responseBody) {
        JsonNode root = parse(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("thought").asBoolean(false)) continue;
            text.append(part.path("text").asText(""));
        }
        if (text.isEmpty()) {
            log.warn("Gemini respondió sin texto: finishReason={}, blockReason={}",
                    root.path("candidates").path(0).path("finishReason").asText("-"),
                    root.path("promptFeedback").path("blockReason").asText("-"));
            throw new BusinessException(ErrorCode.AI_BAD_RESPONSE);
        }
        return text.toString().trim();
    }

    /** Turns an HTTP error of the API into a message the user understands. */
    BusinessException failure(int status, String body, String model) {
        String detail = parseQuietly(body).path("error").path("message").asText(body);
        log.warn("Gemini HTTP {}: {}", status, detail);
        if (status == 429) return new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED);
        if (status == 401 || status == 403 || (status == 400 && detail.toLowerCase().contains("api key"))) {
            return new BusinessException(ErrorCode.AI_KEY_INVALID);
        }
        if (status == 404) return new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND, model);
        if (status >= 500) return new BusinessException(ErrorCode.AI_UNAVAILABLE, status);
        return new BusinessException(ErrorCode.AI_BAD_RESPONSE);
    }

    private JsonNode parse(String text) {
        try {
            return json.readTree(text);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_BAD_RESPONSE);
        }
    }

    private JsonNode parseQuietly(String text) {
        try {
            return json.readTree(text == null ? "{}" : text);
        } catch (JsonProcessingException e) {
            return json.createObjectNode();
        }
    }
}
