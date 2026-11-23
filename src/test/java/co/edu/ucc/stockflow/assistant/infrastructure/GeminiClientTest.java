package co.edu.ucc.stockflow.assistant.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.assistant.application.AiSettings;
import co.edu.ucc.stockflow.assistant.application.Attachment;
import co.edu.ucc.stockflow.settings.application.SettingsService;
import co.edu.ucc.stockflow.settings.domain.SettingRepository;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Talks to a local HTTP server that plays Gemini: checks the real request and response handling. */
class GeminiClientTest {

    private HttpServer server;
    private int status = 200;
    private String answer;
    private String receivedPath;
    private String receivedKey;
    private JsonNode receivedBody;
    private AiSettings settings;
    private GeminiClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedPath = exchange.getRequestURI().getPath();
            receivedKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            receivedBody = new ObjectMapper().readTree(exchange.getRequestBody());
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        Map<String, String> values = new HashMap<>();
        SettingRepository memory = new SettingRepository() {
            public Optional<String> find(String key) { return Optional.ofNullable(values.get(key)); }
            public void save(String key, String value) { values.put(key, value); }
        };
        settings = new AiSettings(new SettingsService(memory), variable -> null);
        settings.save("clave-de-prueba-123", "gemini-prueba");
        client = new GeminiClient(settings, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta/",
                HttpClient.newHttpClient());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static String candidate(String text) {
        return "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":"
                + new ObjectMapper().valueToTree(text) + "}]},\"finishReason\":\"STOP\"}]}";
    }

    @Test
    void sendsTheFileTheSchemaAndTheKeyInTheHeader() {
        answer = candidate("{\"total\": 1500}");
        String result = client.generateJson("Lea la factura",
                List.of(new Attachment("image/png", new byte[]{1, 2, 3})), "{\"type\":\"object\"}");

        assertEquals("{\"total\": 1500}", result);
        assertEquals("/v1beta/models/gemini-prueba:generateContent", receivedPath);
        assertEquals("clave-de-prueba-123", receivedKey);
        JsonNode parts = receivedBody.path("contents").path(0).path("parts");
        assertEquals("image/png", parts.path(0).path("inlineData").path("mimeType").asText());
        assertEquals("AQID", parts.path(0).path("inlineData").path("data").asText());
        assertEquals("Lea la factura", parts.path(1).path("text").asText());
        JsonNode config = receivedBody.path("generationConfig");
        assertEquals("application/json", config.path("responseMimeType").asText());
        assertEquals("object", config.path("responseJsonSchema").path("type").asText());
        assertFalse(receivedPath.contains("clave"), "the key never goes in the URL");
    }

    @Test
    void skipsThoughtsAndJoinsTheTextParts() {
        answer = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"pensando...\",\"thought\":true},"
                + "{\"text\":\"Hola \"},{\"text\":\"mundo\"}]}}]}";
        assertEquals("Hola mundo", client.generateText("Saluda"));
    }

    @Test
    void httpErrorsBecomeClearMessages() {
        assertEquals(ErrorCode.AI_QUOTA_EXCEEDED, errorFor(429, "{\"error\":{\"message\":\"Resource exhausted\"}}"));
        assertEquals(ErrorCode.AI_KEY_INVALID, errorFor(400, "{\"error\":{\"message\":\"API key not valid. Please pass a valid API key.\"}}"));
        assertEquals(ErrorCode.AI_KEY_INVALID, errorFor(403, "{}"));
        assertEquals(ErrorCode.AI_MODEL_NOT_FOUND, errorFor(404, "{\"error\":{\"message\":\"models/x is not found\"}}"));
        assertEquals(ErrorCode.AI_UNAVAILABLE, errorFor(503, "no es json"));
    }

    private ErrorCode errorFor(int httpStatus, String body) {
        status = httpStatus;
        answer = body;
        return assertThrows(BusinessException.class, () -> client.generateText("x")).getCode();
    }

    @Test
    void anAnswerWithoutTextIsABadResponse() {
        answer = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        assertEquals(ErrorCode.AI_BAD_RESPONSE,
                assertThrows(BusinessException.class, () -> client.generateText("x")).getCode());
    }

    @Test
    void withoutKeyItDoesNotCallTheServer() {
        settings.save("", null);
        assertFalse(client.isConfigured());
        assertEquals(ErrorCode.AI_NOT_CONFIGURED,
                assertThrows(BusinessException.class, () -> client.generateText("x")).getCode());
        assertEquals(null, receivedPath);
        assertTrue(client.description().endsWith(AiSettings.DEFAULT_MODEL));
    }
}
