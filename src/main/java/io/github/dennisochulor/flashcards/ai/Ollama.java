package io.github.dennisochulor.flashcards.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dennisochulor.flashcards.config.ModConfig;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal blocking client for a local Ollama server (https://ollama.com).
 * All methods do network I/O and must NOT be called on the render thread.
 */
public final class Ollama {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    /** Keep the model in memory between cards so grading doesn't wait for it to load each time. */
    private static final String KEEP_ALIVE = "30m";

    private final String baseUrl;

    public Ollama(String baseUrl) {
        String url = baseUrl.strip();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Thrown for anything that prevents grading; the message is shown to the player. */
    public static final class OllamaException extends Exception {
        public OllamaException(String message) { super(message); }
        public OllamaException(String message, Throwable cause) { super(message, cause); }
    }

    /** @param status one of {@link GradingPrompt#CORRECT}, PARTIAL, MISSING, WRONG */
    public record KeyPoint(String point, String status) {}

    /**
     * @param score     0 (wrong) to 1 (fully correct), inside the band of {@code ease}
     * @param ease      the AI's grade as an Anki ease: 1 = Again ... 4 = Easy
     * @param vague     the answer is too general to judge
     * @param vagueHint what to be more specific about (doesn't give the answer away)
     */
    public record Grade(double score, int ease, String feedback, boolean vague, String vagueHint, List<KeyPoint> keyPoints) {}

    /** @return the names of the installed models, e.g. "qwen2.5:7b" */
    public List<String> models() throws OllamaException {
        JsonObject json = send(HttpRequest.newBuilder(uri("/api/tags")).timeout(Duration.ofSeconds(5)).GET().build(), "list models");
        List<String> names = new ArrayList<>();
        JsonArray models = json.getAsJsonArray("models");
        if (models != null) models.forEach(m -> names.add(m.getAsJsonObject().get("name").getAsString()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Loads the model into memory ahead of time (Ollama loads a model when it gets a request without a prompt). */
    public void preload(String model) throws OllamaException {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("keep_alive", KEEP_ALIVE);
        send(post("/api/generate", body, Duration.ofSeconds(120)), "load " + model);
    }

    public Grade grade(ModConfig.AiGrading ai, String question, String reference, String answer) throws OllamaException {
        String model = ai.model();
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", false);
        body.addProperty("keep_alive", KEEP_ALIVE);

        JsonArray messages = new JsonArray();
        messages.add(message("system", GradingPrompt.system(ai)));
        messages.add(message("user", GradingPrompt.user(question, reference, answer)));
        body.add("messages", messages);
        body.add("format", GradingPrompt.schema()); // structured output: Ollama forces the reply into this JSON shape

        JsonObject options = new JsonObject();
        options.addProperty("temperature", 0); // the same answer should get the same grade
        body.add("options", options);

        JsonObject json = send(post("/api/chat", body, Duration.ofSeconds(180)), "grade the answer");
        JsonObject message = json.getAsJsonObject("message");
        if (message == null || !message.has("content")) throw new OllamaException("Ollama sent an empty reply.");
        return GradingPrompt.parse(message.get("content").getAsString(), ai);
    }

    private static JsonObject message(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }

    private URI uri(String path) throws OllamaException {
        try {
            return URI.create(baseUrl + path);
        }
        catch (IllegalArgumentException e) {
            throw new OllamaException("Invalid Ollama address: " + baseUrl, e);
        }
    }

    private HttpRequest post(String path, JsonObject body, Duration timeout) throws OllamaException {
        return HttpRequest.newBuilder(uri(path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    private static JsonObject send(HttpRequest request, String what) throws OllamaException {
        HttpResponse<String> response;
        try {
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (ConnectException | HttpConnectTimeoutException e) {
            throw new OllamaException("Can't reach Ollama. Is the Ollama app running?", e);
        }
        catch (HttpTimeoutException e) {
            throw new OllamaException("Ollama took too long to " + what + ".", e);
        }
        catch (IOException e) {
            throw new OllamaException("Couldn't talk to Ollama: " + e.getMessage(), e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaException("Interrupted while talking to Ollama.", e);
        }

        JsonObject json = null;
        try {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) json = parsed.getAsJsonObject();
        }
        catch (RuntimeException ignored) {
            // handled below
        }
        @Nullable String error = json != null && json.has("error") ? json.get("error").getAsString() : null;
        if (response.statusCode() != 200 || error != null) {
            if (error != null && error.contains("not found")) {
                throw new OllamaException("Ollama: " + error + ". Install it with \"ollama pull <model>\" or pick another model.");
            }
            throw new OllamaException("Ollama couldn't " + what + ": " + (error != null ? error : "HTTP " + response.statusCode()));
        }
        if (json == null) throw new OllamaException("Ollama sent an unexpected response.");
        return json;
    }
}
