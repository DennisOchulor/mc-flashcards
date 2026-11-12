package io.github.dennisochulor.flashcards.anki;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Minimal blocking client for the AnkiConnect add-on (https://ankiweb.net/shared/info/2055492159).
 * All methods do network I/O and must NOT be called on the render thread.
 */
public final class AnkiConnect {
    private static final int API_VERSION = 6;
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1) // AnkiConnect's tiny server doesn't speak HTTP/2
            .build();

    private final URI endpoint;

    public AnkiConnect(String url) {
        this.endpoint = URI.create(url);
    }

    /** Thrown for anything that prevents talking to Anki; the message is shown to the player. */
    public static final class AnkiException extends Exception {
        public AnkiException(String message) { super(message); }
        public AnkiException(String message, Throwable cause) { super(message, cause); }
    }

    /** The card Anki's reviewer is currently showing. */
    public record CurrentCard(long cardId, String deckName, String questionHtml, String answerHtml,
                              List<Integer> buttons, List<String> nextReviews) {}

    public int version() throws AnkiException {
        return invoke("version", null, Duration.ofSeconds(3)).getAsInt();
    }

    public List<String> deckNames() throws AnkiException {
        List<String> names = new ArrayList<>();
        invoke("deckNames", null, Duration.ofSeconds(5)).getAsJsonArray().forEach(e -> names.add(e.getAsString()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Opens the deck in Anki's reviewer. Returns false when the deck has nothing left to study today (or doesn't exist). */
    public boolean guiDeckReview(String deck) throws AnkiException {
        return invoke("guiDeckReview", Map.of("name", deck), Duration.ofSeconds(10)).getAsBoolean();
    }

    /** @return the card currently shown by Anki's reviewer, or null if the reviewer has no card. */
    public @Nullable CurrentCard guiCurrentCard() throws AnkiException {
        JsonElement result;
        try {
            result = invoke("guiCurrentCard", null, Duration.ofSeconds(10));
        }
        catch (AnkiException e) {
            // AnkiConnect raises "Gui review is not currently active." when the reviewer closed (e.g. deck finished)
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not currently active")) return null;
            throw e;
        }
        if (result == null || result.isJsonNull()) return null;

        JsonObject o = result.getAsJsonObject();
        List<Integer> buttons = new ArrayList<>();
        o.getAsJsonArray("buttons").forEach(e -> buttons.add(e.getAsInt()));
        List<String> nextReviews = new ArrayList<>();
        if (o.has("nextReviews")) o.getAsJsonArray("nextReviews").forEach(e -> nextReviews.add(AnkiHtml.decodeEntities(
                AnkiHtml.BIDI_CONTROLS.matcher(e.getAsString()).replaceAll("")).strip()));
        return new CurrentCard(
                o.get("cardId").getAsLong(),
                o.has("deckName") ? o.get("deckName").getAsString() : "",
                o.get("question").getAsString(),
                o.get("answer").getAsString(),
                buttons, nextReviews);
    }

    /** Due counts for a deck, after Anki's daily limits (what Anki's deck list shows). */
    public record DeckCounts(int newCount, int learnCount, int reviewCount) {}

    public int numCardsReviewedToday() throws AnkiException {
        return invoke("getNumCardsReviewedToday", null, Duration.ofSeconds(5)).getAsInt();
    }

    /** @return the deck's due counts, or null if Anki doesn't know the deck */
    public @Nullable DeckCounts deckCounts(String deck) throws AnkiException {
        JsonElement result = invoke("getDeckStats", Map.of("decks", List.of(deck)), Duration.ofSeconds(5));
        if (result == null || !result.isJsonObject()) return null;
        for (Map.Entry<String, JsonElement> entry : result.getAsJsonObject().entrySet()) {
            JsonObject stats = entry.getValue().getAsJsonObject();
            if (stats.has("name") && !stats.get("name").getAsString().equals(deck)) continue;
            return new DeckCounts(stats.get("new_count").getAsInt(), stats.get("learn_count").getAsInt(), stats.get("review_count").getAsInt());
        }
        return null;
    }

    public boolean guiShowAnswer() throws AnkiException {
        return invoke("guiShowAnswer", null, Duration.ofSeconds(10)).getAsBoolean();
    }

    /** @param ease 1 = Again, 2 = Hard, 3 = Good, 4 = Easy */
    public boolean guiAnswerCard(int ease) throws AnkiException {
        return invoke("guiAnswerCard", Map.of("ease", ease), Duration.ofSeconds(10)).getAsBoolean();
    }

    /** @return the media file's bytes, or null if Anki doesn't have it. */
    public byte @Nullable [] retrieveMediaFile(String filename) throws AnkiException {
        JsonElement result = invoke("retrieveMediaFile", Map.of("filename", filename), Duration.ofSeconds(15));
        if (result == null || result.isJsonNull() || !result.isJsonPrimitive() || !result.getAsJsonPrimitive().isString()) return null;
        return Base64.getDecoder().decode(result.getAsString());
    }

    /** Syncs the collection with AnkiWeb (the user must be logged in to AnkiWeb inside Anki). */
    public void sync() throws AnkiException {
        invoke("sync", null, Duration.ofSeconds(120));
    }

    private @Nullable JsonElement invoke(String action, @Nullable Map<String, ?> params, Duration timeout) throws AnkiException {
        JsonObject body = new JsonObject();
        body.addProperty("action", action);
        body.addProperty("version", API_VERSION);
        if (params != null) body.add("params", GSON.toJsonTree(params));

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (ConnectException | HttpConnectTimeoutException e) {
            throw new AnkiException("Can't reach Anki. Is Anki open with the AnkiConnect add-on installed?", e);
        }
        catch (HttpTimeoutException e) {
            throw new AnkiException("Anki took too long to respond (" + action + ").", e);
        }
        catch (IOException e) {
            throw new AnkiException("Couldn't talk to Anki: " + e.getMessage(), e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnkiException("Interrupted while talking to Anki.", e);
        }

        if (response.statusCode() != 200) {
            throw new AnkiException("AnkiConnect returned HTTP " + response.statusCode() + " for " + action + ".");
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(response.body()).getAsJsonObject();
        }
        catch (RuntimeException e) {
            throw new AnkiException("AnkiConnect sent an unexpected response for " + action + ".", e);
        }
        JsonElement error = json.get("error");
        if (error != null && !error.isJsonNull()) {
            throw new AnkiException("Anki: " + error.getAsString());
        }
        return json.get("result");
    }
}
