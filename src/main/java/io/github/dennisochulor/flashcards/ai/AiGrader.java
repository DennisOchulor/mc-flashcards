package io.github.dennisochulor.flashcards.ai;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.Flashcards;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runs Ollama requests on a background thread and hands results back to the client thread.
 * Separate from the Anki thread so a slow grade never delays sending reviews to Anki.
 */
public final class AiGrader {
    private AiGrader() {}

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Flashcards-Ollama");
        t.setDaemon(true);
        return t;
    });

    /** AI grading is used only for Anki cards, and only once a model is picked. */
    public static boolean isEnabled() {
        ModConfig config = FileManager.getConfig();
        return config.ankiMode() && config.aiGrading().enabled() && !config.aiGrading().model().isBlank();
    }

    /** Starts loading the model while the player is still typing, so grading is quick. Errors are ignored here. */
    public static void preload() {
        ModConfig.AiGrading ai = FileManager.getConfig().aiGrading();
        EXECUTOR.execute(() -> {
            try {
                new Ollama(ai.ollamaUrl()).preload(ai.model());
            }
            catch (Ollama.OllamaException | RuntimeException e) {
                Flashcards.LOGGER.debug("Couldn't preload Ollama model {}", ai.model(), e);
            }
        });
    }

    /** Grades the answer; exactly one callback runs, on the client thread. */
    public static void grade(String question, String reference, String answer,
                             Consumer<Ollama.Grade> onSuccess, Consumer<String> onError) {
        ModConfig.AiGrading ai = FileManager.getConfig().aiGrading();
        EXECUTOR.execute(() -> {
            try {
                Ollama.Grade grade = new Ollama(ai.ollamaUrl()).grade(ai, question, reference, answer);
                Minecraft.getInstance().execute(() -> onSuccess.accept(grade));
            }
            catch (Ollama.OllamaException e) {
                Flashcards.LOGGER.warn("AI grading failed", e);
                Minecraft.getInstance().execute(() -> onError.accept(e.getMessage()));
            }
            catch (RuntimeException e) {
                Flashcards.LOGGER.error("Unexpected error during AI grading", e);
                Minecraft.getInstance().execute(() -> onError.accept("Unexpected error while grading, see the log."));
            }
        });
    }

    /** Loads the installed models for the config screen; exactly one callback runs, on the client thread. */
    public static void loadModels(String url, Consumer<List<String>> onSuccess, Consumer<String> onError) {
        EXECUTOR.execute(() -> {
            try {
                List<String> models = new Ollama(url).models();
                Minecraft.getInstance().execute(() -> onSuccess.accept(models));
            }
            catch (Ollama.OllamaException e) {
                Minecraft.getInstance().execute(() -> onError.accept(e.getMessage()));
            }
            catch (RuntimeException e) {
                Minecraft.getInstance().execute(() -> onError.accept("Couldn't load models: " + e.getMessage()));
            }
        });
    }
}
