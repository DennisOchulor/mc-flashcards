package io.github.dennisochulor.flashcards.config;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.List;

public record ModConfig(int interval, boolean intervalToggle, Boolean validationToggle,
                        HashMap<String,Boolean> categoryToggle, List<String> correctAnswerCommands,
                        List<String> wrongAnswerCommands, CommandSelectionStrategy commandSelectionStrategy,
                        Boolean ankiMode, String ankiDeck, String ankiConnectUrl, Boolean ankiSync,
                        TimerMode timerMode, Boolean hudEnabled, HudPosition hudPosition, AiGrading aiGrading,
                        Integer intervalSeconds)
{
    public static final String DEFAULT_ANKI_CONNECT_URL = "http://127.0.0.1:8765";

    public enum CommandSelectionStrategy {
        EXECUTE_ALL("Execute All", "Execute all listed commands in sequential order."),
        RANDOMISE_ONE("Randomise One", "Randomly select one command."),
        OFF("OFF", "Don't execute commands.");

        public final String friendlyName;
        public final Tooltip tooltip;

        CommandSelectionStrategy(String friendlyName, String tooltip) {
            this.friendlyName = friendlyName;
            this.tooltip = Tooltip.create(Component.nullToEmpty(tooltip));
        }

        public static CommandSelectionStrategy fromFriendlyName(String str) {
            return switch(str) {
                case "Execute All" -> EXECUTE_ALL;
                case "Randomise One" -> RANDOMISE_ONE;
                case "OFF" -> OFF;
                default -> throw new IllegalArgumentException("Unknown str: " + str);
            };
        }
    }

    public static final String DEFAULT_OLLAMA_URL = "http://127.0.0.1:11434";

    /**
     * Grading typed answers with a local AI (Ollama). The score (0-1) unlocks Anki's buttons:
     * Again is always allowed, Hard from {@code hardFrom}, Good from {@code goodFrom}, Easy from {@code easyFrom}.
     *
     * @param adjustScore       whether the player may change the AI's score before grading
     * @param quitAfterSeconds  how long the AI may think before a Quit button lets the player keep playing meanwhile
     * @param indicatorPosition where the "AI is thinking" indicator is drawn while playing
     * @param minPlaySeconds    after "Keep playing", the card comes back no sooner than this
     * @param autoKeepPlaying   go back to the game as soon as the AI starts thinking, without pressing "Keep playing"
     */
    public record AiGrading(Boolean enabled, String ollamaUrl, String model,
                            Double hardFrom, Double goodFrom, Double easyFrom, Boolean adjustScore,
                            Integer quitAfterSeconds, IndicatorPosition indicatorPosition,
                            Integer minPlaySeconds, Boolean autoKeepPlaying) {
        public static final double DEFAULT_HARD_FROM = 0.5, DEFAULT_GOOD_FROM = 0.7, DEFAULT_EASY_FROM = 0.9;
        public static final int DEFAULT_QUIT_AFTER_SECONDS = 15, MAX_QUIT_AFTER_SECONDS = 120;
        public static final int DEFAULT_MIN_PLAY_SECONDS = 5, MAX_MIN_PLAY_SECONDS = 120;
        public static final AiGrading DEFAULT = new AiGrading(false, DEFAULT_OLLAMA_URL, "",
                DEFAULT_HARD_FROM, DEFAULT_GOOD_FROM, DEFAULT_EASY_FROM, false,
                DEFAULT_QUIT_AFTER_SECONDS, IndicatorPosition.CENTER, DEFAULT_MIN_PLAY_SECONDS, false);

        @SuppressWarnings("ConstantValue")
        public AiGrading {
            // null checks needed for when Gson deserializes
            if (enabled == null) enabled = false;
            if (ollamaUrl == null || ollamaUrl.isBlank()) ollamaUrl = DEFAULT_OLLAMA_URL;
            if (model == null) model = "";
            if (hardFrom == null) hardFrom = DEFAULT_HARD_FROM;
            if (goodFrom == null) goodFrom = DEFAULT_GOOD_FROM;
            if (easyFrom == null) easyFrom = DEFAULT_EASY_FROM;
            if (adjustScore == null) adjustScore = false;
            if (quitAfterSeconds == null) quitAfterSeconds = DEFAULT_QUIT_AFTER_SECONDS;
            quitAfterSeconds = Math.max(0, Math.min(MAX_QUIT_AFTER_SECONDS, quitAfterSeconds));
            if (indicatorPosition == null) indicatorPosition = IndicatorPosition.CENTER;
            if (minPlaySeconds == null) minPlaySeconds = DEFAULT_MIN_PLAY_SECONDS;
            minPlaySeconds = Math.max(0, Math.min(MAX_MIN_PLAY_SECONDS, minPlaySeconds));
            if (autoKeepPlaying == null) autoKeepPlaying = false;
            hardFrom = clamp01(hardFrom);
            goodFrom = Math.max(hardFrom, clamp01(goodFrom)); // keep Hard <= Good <= Easy
            easyFrom = Math.max(goodFrom, clamp01(easyFrom));
        }

        private static double clamp01(double d) {
            return Math.round(Math.min(1, Math.max(0, d)) * 100) / 100.0;
        }

        /** @return the lowest score that unlocks the button for this Anki ease (1 = Again ... 4 = Easy) */
        public double minimumScore(int ease) {
            return switch (ease) {
                case 1 -> 0;
                case 2 -> hardFrom;
                case 3 -> goodFrom;
                default -> easyFrom;
            };
        }
    }

    /** Where the "AI is thinking" indicator is drawn while the player keeps playing. */
    public enum IndicatorPosition {
        CENTER("Center"), TOP_CENTER("Top center"), TOP_LEFT("Top left"), TOP_RIGHT("Top right");

        public final String friendlyName;

        IndicatorPosition(String friendlyName) {
            this.friendlyName = friendlyName;
        }
    }

    /** What the question interval is measured in. */
    public enum TimerMode {
        REAL_TIME("Real time", "Counts real-world minutes, even while the game is paused or a menu is open."),
        GAME_TIME("Game time", "Counts only while the world is running: stops while the game is paused and follows /tick rate.\n" +
                "1 minute = 1200 game ticks (a full Minecraft day is 20 minutes).");

        public final String friendlyName;
        public final String description;

        TimerMode(String friendlyName, String description) {
            this.friendlyName = friendlyName;
            this.description = description;
        }
    }

    /** Screen corner the timer/stats HUD is drawn in. */
    public enum HudPosition {
        TOP_LEFT("Top left"), TOP_RIGHT("Top right"), BOTTOM_LEFT("Bottom left"), BOTTOM_RIGHT("Bottom right");

        public final String friendlyName;

        HudPosition(String friendlyName) {
            this.friendlyName = friendlyName;
        }
    }

    @SuppressWarnings("ConstantValue")
    public ModConfig {
        // interval = minutes, intervalSeconds = the extra 0-59 seconds
        if (intervalSeconds == null) intervalSeconds = 0;
        if (interval < 0 || intervalSeconds < 0 || intervalSeconds > 59) throw new IllegalArgumentException("invalid interval!");
        if (interval == 0 && intervalSeconds == 0) throw new IllegalArgumentException("interval must be at least 1 second!");
        // null checks needed for when Gson deserializes
        if (correctAnswerCommands == null) correctAnswerCommands = List.of();
        if (wrongAnswerCommands == null) wrongAnswerCommands = List.of();
        if (validationToggle == null) validationToggle = true;
        if (commandSelectionStrategy == null) commandSelectionStrategy = CommandSelectionStrategy.EXECUTE_ALL;
        if (ankiMode == null) ankiMode = false;
        if (ankiDeck == null) ankiDeck = "";
        if (ankiConnectUrl == null || ankiConnectUrl.isBlank()) ankiConnectUrl = DEFAULT_ANKI_CONNECT_URL;
        if (ankiSync == null) ankiSync = true;
        if (timerMode == null) timerMode = TimerMode.REAL_TIME;
        if (hudEnabled == null) hudEnabled = true;
        if (hudPosition == null) hudPosition = HudPosition.TOP_LEFT;
        if (aiGrading == null) aiGrading = AiGrading.DEFAULT;

        correctAnswerCommands = correctAnswerCommands.stream().filter(s -> !s.isBlank()).map(s -> {
            if (s.startsWith("/")) s = s.substring(1);
            return s;
        }).toList();
        wrongAnswerCommands = wrongAnswerCommands.stream().filter(s -> !s.isBlank()).map(s -> {
            if (s.startsWith("/")) s = s.substring(1);
            return s;
        }).toList();
    }

    /** @return the time between questions, in seconds */
    public int intervalTotalSeconds() {
        return interval * 60 + intervalSeconds;
    }

    public ModConfig withInterval(int interval, int intervalSeconds, boolean intervalToggle) {
        return new ModConfig(interval, intervalToggle, validationToggle, categoryToggle, correctAnswerCommands,
                wrongAnswerCommands, commandSelectionStrategy, ankiMode, ankiDeck, ankiConnectUrl, ankiSync, timerMode, hudEnabled, hudPosition, aiGrading, intervalSeconds);
    }

    public ModConfig withValidationToggle(boolean validationToggle) {
        return new ModConfig(interval, intervalToggle, validationToggle, categoryToggle, correctAnswerCommands,
                wrongAnswerCommands, commandSelectionStrategy, ankiMode, ankiDeck, ankiConnectUrl, ankiSync, timerMode, hudEnabled, hudPosition, aiGrading, intervalSeconds);
    }

    public ModConfig withCategoryToggle(HashMap<String, Boolean> categoryToggle) {
        return new ModConfig(interval, intervalToggle, validationToggle, categoryToggle, correctAnswerCommands,
                wrongAnswerCommands, commandSelectionStrategy, ankiMode, ankiDeck, ankiConnectUrl, ankiSync, timerMode, hudEnabled, hudPosition, aiGrading, intervalSeconds);
    }

    public ModConfig withCommands(List<String> correctAnswerCommands, List<String> wrongAnswerCommands, CommandSelectionStrategy commandSelectionStrategy) {
        return new ModConfig(interval, intervalToggle, validationToggle, categoryToggle, correctAnswerCommands,
                wrongAnswerCommands, commandSelectionStrategy, ankiMode, ankiDeck, ankiConnectUrl, ankiSync, timerMode, hudEnabled, hudPosition, aiGrading, intervalSeconds);
    }

    public ModConfig withAnki(boolean ankiMode, String ankiDeck, String ankiConnectUrl, boolean ankiSync) {
        return new ModConfig(interval, intervalToggle, validationToggle, categoryToggle, correctAnswerCommands,
                wrongAnswerCommands, commandSelectionStrategy, ankiMode, ankiDeck, ankiConnectUrl, ankiSync, timerMode, hudEnabled, hudPosition, aiGrading, intervalSeconds);
    }

    public ModConfig withAiGrading(AiGrading aiGrading) {
        return new ModConfig(interval, intervalToggle, validationToggle, categoryToggle, correctAnswerCommands,
                wrongAnswerCommands, commandSelectionStrategy, ankiMode, ankiDeck, ankiConnectUrl, ankiSync, timerMode, hudEnabled, hudPosition, aiGrading, intervalSeconds);
    }

    public ModConfig withTimerAndHud(TimerMode timerMode, boolean hudEnabled, HudPosition hudPosition) {
        return new ModConfig(interval, intervalToggle, validationToggle, categoryToggle, correctAnswerCommands,
                wrongAnswerCommands, commandSelectionStrategy, ankiMode, ankiDeck, ankiConnectUrl, ankiSync, timerMode, hudEnabled, hudPosition, aiGrading, intervalSeconds);
    }
}
