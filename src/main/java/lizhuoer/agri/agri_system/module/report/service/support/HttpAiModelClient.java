package lizhuoer.agri.agri_system.module.report.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(prefix = "report.ai", name = "enabled", havingValue = "true")
public class HttpAiModelClient implements AiModelClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${report.ai.enabled:false}")
    private boolean enabled;

    @Value("${report.ai.base-url:}")
    private String baseUrl;

    @Value("${report.ai.api-key:}")
    private String apiKey;

    @Value("${report.ai.model:}")
    private String model;

    @Value("${report.ai.path:/v1/chat/completions}")
    private String path;

    @Value("${report.ai.timeout-seconds:60}")
    private long timeoutSeconds;

    private HttpClient httpClient;

    public HttpAiModelClient() {
        // Spring field injection path.
    }

    public HttpAiModelClient(boolean enabled,
                             String baseUrl,
                             String apiKey,
                             String model,
                             String path,
                             long timeoutSeconds) {
        this(enabled, baseUrl, apiKey, model, path, timeoutSeconds, buildHttpClient(timeoutSeconds));
    }

    HttpAiModelClient(boolean enabled,
                      String baseUrl,
                      String apiKey,
                      String model,
                      String path,
                      long timeoutSeconds,
                      HttpClient httpClient) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.path = path;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = httpClient;
        normalizeAndValidate();
    }

    @PostConstruct
    void initialize() {
        normalizeAndValidate();
        if (httpClient == null) {
            httpClient = buildHttpClient(timeoutSeconds);
        }
    }

    @Override
    public void stream(String prompt,
                       Consumer<String> deltaConsumer,
                       Runnable completionCallback,
                       Consumer<Throwable> errorCallback) {
        if (!enabled) {
            errorCallback.accept(new IllegalStateException("report.ai provider disabled"));
            return;
        }
        try {
            HttpRequest request = buildRequest(prompt);
            HttpResponse<InputStream> response = send(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                errorCallback.accept(new IllegalStateException("AI provider returned HTTP " + response.statusCode()));
                return;
            }
            boolean seenDone = consumeStream(response.body(), deltaConsumer);
            if (!seenDone) {
                errorCallback.accept(new IllegalStateException("AI provider stream ended before [DONE] marker"));
                return;
            }
            completionCallback.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            errorCallback.accept(exception);
        } catch (IOException | RuntimeException exception) {
            errorCallback.accept(exception);
        }
    }

    private HttpRequest buildRequest(String prompt) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream");
        if (!apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }
        return requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt)))
                .build();
    }

    private HttpResponse<InputStream> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private boolean consumeStream(InputStream responseBody, Consumer<String> deltaConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    return true;
                }
                JsonNode contentNode = OBJECT_MAPPER.readTree(payload)
                        .path("choices")
                        .path(0)
                        .path("delta")
                        .path("content");
                if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                    String delta = contentNode.asText();
                    if (!delta.isEmpty()) {
                        deltaConsumer.accept(delta);
                    }
                }
            }
        }
        return false;
    }

    private String buildRequestBody(String prompt) throws IOException {
        JsonNode body = OBJECT_MAPPER.createObjectNode()
                .put("model", model)
                .put("stream", true)
                .set("messages", OBJECT_MAPPER.createArrayNode().add(
                        OBJECT_MAPPER.createObjectNode()
                                .put("role", "user")
                                .put("content", prompt)
                ));
        return OBJECT_MAPPER.writeValueAsString(body);
    }

    private void normalizeAndValidate() {
        baseUrl = trimToEmpty(baseUrl);
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        apiKey = trimToEmpty(apiKey);
        model = trimToEmpty(model);
        path = normalizePath(path);

        if (!enabled) {
            return;
        }
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("report.ai base-url must not be blank when provider is enabled");
        }
        if (model.isBlank()) {
            throw new IllegalStateException("report.ai model must not be blank when provider is enabled");
        }
    }

    private static HttpClient buildHttpClient(long timeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    private static String normalizePath(String value) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isEmpty()) {
            return "/v1/chat/completions";
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
