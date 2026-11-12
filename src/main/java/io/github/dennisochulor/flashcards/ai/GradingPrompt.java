package io.github.dennisochulor.flashcards.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dennisochulor.flashcards.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the model is told, the JSON shape it must reply with, and how that reply is read.
 * <p>
 * The grade definitions here are the same ones the player sees in the button tooltips ({@link #DEFINITIONS}),
 * and the score bands use the player's own cut-offs, so the AI's score and Anki's buttons always mean the same thing.
 */
public final class GradingPrompt {
    private GradingPrompt() {}

    /** Shared definitions, also shown to the player. Index = Anki ease - 1. */
    public static final String[] DEFINITIONS = {
            "Again: you didn't remember it. The core idea is missing or wrong, a key point is factually wrong, "
                    + "the answer is too vague to show understanding, or you didn't know.",
            "Hard: you remembered only part of it. The core idea is right, but a key point is missing, "
                    + "or something the question explicitly asks for (an example, an analogy, a formula...) is missing.",
            "Good: you remembered it properly. The core idea and most key points are right; only minor details "
                    + "or exact terms are missing, and nothing is wrong.",
            "Easy: you know it completely. Every key point the question asks for, stated precisely, no gaps.",
    };

    public static final String CORRECT = "correct", PARTIAL = "partial", MISSING = "missing", WRONG = "wrong";

    static String system(ModConfig.AiGrading ai) {
        String h = num(ai.hardFrom()), g = num(ai.goodFrom()), e = num(ai.easyFrom());
        return """
                You grade a student's flashcard answer during spaced-repetition review (Anki).
                You get the QUESTION, the REFERENCE answer from the flashcard, and the STUDENT answer.

                Grade how much of what the QUESTION asks for the student actually remembered, by meaning, not wording. \
                Synonyms, paraphrases, a different order and different examples are all fine. The reference may contain \
                more than the question asks for: anything the question doesn't ask for is a bonus, never a requirement. \
                How long the student took doesn't matter.

                Work in this order:
                1. "key_points": the 2-6 key points the QUESTION asks for (use the reference to know them). Label each with \
                a short neutral phrase (e.g. "what makes the line the best one"), and mark it "correct", "partial", \
                "missing" or "wrong".
                2. "vague": true ONLY when the answer is on the right track but too general or imprecise to show the \
                student really knows it: there is something true and relevant in it, it just isn't pinned down. \
                Use the key points: vague needs at least one point "correct" or "partial" and none "wrong". Examples:
                - "It finds the best line through the points" (asked what least squares does): vague. True, but doesn't \
                say what makes a line the best.
                - "I dunno", "no idea", "skip", blank, jokes, excuses: NOT vague. There is nothing to sharpen.
                - "Least squares estimation is a residual": NOT vague. It's wrong.
                - An answer about something else, or to a different question: NOT vague. It's off-topic.
                - Short but precise and correct: NOT vague. Short is fine.
                "vague_hint": only when vague, one sentence telling the student what to pin down, WITHOUT giving away \
                the answer (e.g. "Say what makes one line better than another."). Otherwise an empty string.
                3. "grade", using exactly these definitions:
                - "again": didn't remember it. The core idea is missing or wrong, a key point is factually wrong, \
                the answer is too vague to show understanding, or it's blank / "I don't know".
                - "hard": remembered only part of it. The core idea is right, but at least one key point is missing, \
                or something the question explicitly asks for (an example, an analogy, a formula...) is missing.
                - "good": remembered it properly. The core idea and most key points are right; only minor details or \
                exact terms are missing; nothing is wrong.
                - "easy": knows it completely. Every key point the question asks for, stated precisely, no gaps.
                If the question explicitly asks for something and it's missing, the grade is at most "hard". \
                A factually wrong key point makes it at most "hard" ("again" if it's the core idea).
                4. "score": a number from 0 to 1 inside the grade's band: again below %s, hard %s up to %s, \
                good %s up to %s, easy %s or more. Higher inside a band means closer to the next grade.
                5. "feedback": 1-3 short sentences to the student ("you"): what was right and exactly what was missing \
                or wrong. Plain text, no markdown, no LaTeX.
                Reply with JSON only.""".formatted(h, h, g, g, e, e);
    }

    private static String num(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    static String user(String question, String reference, String answer) {
        return "QUESTION:\n" + question.strip()
                + "\n\nREFERENCE ANSWER:\n" + reference.strip()
                + "\n\nSTUDENT ANSWER:\n" + (answer.isBlank() ? "(blank)" : answer.strip());
    }

    /** JSON schema for Ollama's structured output, in the order the model should think. */
    static JsonObject schema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "key_points": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "point": {"type": "string"},
                          "status": {"type": "string", "enum": ["correct", "partial", "missing", "wrong"]}
                        },
                        "required": ["point", "status"]
                      }
                    },
                    "vague": {"type": "boolean"},
                    "vague_hint": {"type": "string"},
                    "grade": {"type": "string", "enum": ["again", "hard", "good", "easy"]},
                    "score": {"type": "number", "minimum": 0, "maximum": 1},
                    "feedback": {"type": "string"}
                  },
                  "required": ["key_points", "vague", "vague_hint", "grade", "score", "feedback"]
                }""").getAsJsonObject();
    }

    static Ollama.Grade parse(String content, ModConfig.AiGrading ai) throws Ollama.OllamaException {
        String text = content.strip();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) throw new Ollama.OllamaException("The AI didn't reply with a grade. Try another model.");
        try {
            JsonObject json = JsonParser.parseString(text.substring(start, end + 1)).getAsJsonObject();

            JsonElement scoreJson = json.get("score");
            if (scoreJson == null || !scoreJson.isJsonPrimitive()) throw new IllegalStateException("no score");
            double score = scoreJson.getAsDouble();
            if (score > 1.5 && score <= 100) score /= 100; // some models answer in percent; 1.0-1.5 is just clamped
            score = clamp(score, 0, 1);

            int ease = switch (string(json, "grade").toLowerCase(Locale.ROOT)) {
                case "again" -> 1;
                case "hard" -> 2;
                case "good" -> 3;
                case "easy" -> 4;
                default -> easeForScore(score, ai);
            };
            score = clampToBand(score, ease, ai); // the grade's definition wins over a stray number

            List<Ollama.KeyPoint> keyPoints = new ArrayList<>();
            JsonElement points = json.get("key_points");
            if (points != null && points.isJsonArray()) {
                for (JsonElement p : (JsonArray) points) {
                    if (!p.isJsonObject()) continue;
                    String point = string(p.getAsJsonObject(), "point");
                    String status = string(p.getAsJsonObject(), "status").toLowerCase(Locale.ROOT);
                    if (!point.isBlank()) keyPoints.add(new Ollama.KeyPoint(point, status));
                }
            }

            boolean vague = json.has("vague") && json.get("vague").isJsonPrimitive() && json.get("vague").getAsBoolean();
            return new Ollama.Grade(Math.round(score * 100) / 100.0, ease, string(json, "feedback"), vague,
                    string(json, "vague_hint"), keyPoints);
        }
        catch (RuntimeException e) {
            throw new Ollama.OllamaException("Couldn't read the AI's grade. Try again or pick another model.", e);
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().strip() : "";
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(max, Math.max(min, v));
    }

    static int easeForScore(double score, ModConfig.AiGrading ai) {
        if (score >= ai.easyFrom()) return 4;
        if (score >= ai.goodFrom()) return 3;
        if (score >= ai.hardFrom()) return 2;
        return 1;
    }

    /** Moves the score into the band of the given grade, e.g. "hard" with 0.75 becomes 0.69 (with the default cut-offs). */
    static double clampToBand(double score, int ease, ModConfig.AiGrading ai) {
        double min = ai.minimumScore(ease);
        double max = ease == 4 ? 1 : ai.minimumScore(ease + 1) - 0.01;
        if (max < min) return min; // empty band (two cut-offs equal)
        return clamp(score, min, max);
    }
}
