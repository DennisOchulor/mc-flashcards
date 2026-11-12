package io.github.dennisochulor.flashcards;

import io.github.dennisochulor.flashcards.anki.AnkiSession;
import io.github.dennisochulor.flashcards.anki.StudyStats;
import io.github.dennisochulor.flashcards.config.ModConfig;
import io.github.dennisochulor.flashcards.questions.AnkiQuestionScreen;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import io.github.dennisochulor.flashcards.questions.ThinkingAnimation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Small HUD panel: countdown to the next question plus this session's / Anki's study numbers. */
public final class StudyHud {
    private StudyHud() {}

    private static final int MARGIN = 4;
    private static final int PADDING = 3;
    private static final int BACKGROUND = 0x90000000;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int NEW_COLOR = 0xFF6EA8FF;    // Anki's blue
    private static final int LEARN_COLOR = 0xFFFF7070;  // Anki's red
    private static final int REVIEW_COLOR = 0xFF70D070; // Anki's green

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        ModConfig config = FileManager.getConfig();
        if (minecraft.player == null || minecraft.getDebugOverlay().showDebugScreen()) return;

        AnkiQuestionScreen grading = AnkiQuestionScreen.backgroundCard();
        if (grading != null) extractThinking(graphics, minecraft.font, grading, config.aiGrading().indicatorPosition());

        if (!config.hudEnabled()) return;

        List<Component> lines = new ArrayList<>();
        lines.add(timerLine(minecraft, config));

        int total = StudyStats.sessionTotal();
        lines.add(Component.literal("Session: ").withColor(GRAY)
                .append(Component.literal(total + (total == 1 ? " card" : " cards")).withColor(CommonColors.WHITE))
                .append(total == 0 ? Component.empty() : Component.literal("  " + StudyStats.sessionCorrect() + "✔").withColor(CommonColors.GREEN))
                .append(total == 0 ? Component.empty() : Component.literal(" " + StudyStats.sessionWrong() + "✘").withColor(CommonColors.SOFT_RED)));

        if (AnkiSession.isEnabled()) {
            StudyStats.AnkiStats anki = StudyStats.anki();
            if (anki == null) {
                lines.add(Component.literal("Anki: not connected").withColor(GRAY));
            }
            else {
                lines.add(Component.literal("Anki today: ").withColor(GRAY)
                        .append(Component.literal(anki.reviewedToday() + " reviewed").withColor(CommonColors.WHITE)));
                if (anki.hasDeckCounts()) {
                    lines.add(anki.dueTotal() == 0
                            ? Component.literal("Deck done for today ✔").withColor(CommonColors.GREEN)
                            : Component.literal("Due: ").withColor(GRAY)
                                .append(Component.literal(String.valueOf(anki.newCount())).withColor(NEW_COLOR))
                                .append(Component.literal(" new · ").withColor(GRAY))
                                .append(Component.literal(String.valueOf(anki.learnCount())).withColor(LEARN_COLOR))
                                .append(Component.literal(" learn · ").withColor(GRAY))
                                .append(Component.literal(String.valueOf(anki.reviewCount())).withColor(REVIEW_COLOR))
                                .append(Component.literal(" review").withColor(GRAY)));
                }
            }
        }

        draw(graphics, minecraft.font, lines, config.hudPosition());
    }

    private static Component timerLine(Minecraft minecraft, ModConfig config) {
        if (!config.intervalToggle()) {
            @Nullable Component key = Flashcards.promptKeyName();
            return Component.literal("Timer off").withColor(GRAY)
                    .append(key == null ? Component.empty() : Component.literal(" · press ").withColor(GRAY).append(key.copy().withColor(CommonColors.WHITE)));
        }
        return switch (QuestionScheduler.state()) {
            case OFF -> Component.literal("Timer starting...").withColor(GRAY);
            case DUE -> Component.literal("Next card: ").withColor(GRAY).append(Component.literal("now").withColor(CommonColors.YELLOW));
            case QUESTION -> Component.literal(AnkiQuestionScreen.backgroundCard() != null ? "AI is grading..." : "Card on screen").withColor(GRAY);
            case COUNTING -> {
                long millis = QuestionScheduler.remainingMillis();
                boolean gameTime = QuestionScheduler.timerMode() == ModConfig.TimerMode.GAME_TIME;
                MutableComponent line = Component.literal("Next card in ").withColor(GRAY)
                        .append(Component.literal(formatDuration(millis)).withColor(millis <= 10_000 ? CommonColors.YELLOW : CommonColors.WHITE));
                if (gameTime) line.append(Component.literal(minecraft.isPaused() ? " (paused)" : " (game time)").withColor(GRAY));
                yield line;
            }
        };
    }

    /** "AI is thinking" while the player keeps playing after Quit. Center sits below the crosshair so aiming isn't blocked. */
    private static void extractThinking(GuiGraphicsExtractor graphics, Font font, AnkiQuestionScreen card, ModConfig.IndicatorPosition position) {
        int width = ThinkingAnimation.width(font, card.modelName());
        int height = ThinkingAnimation.height(font);
        int boxWidth = width + PADDING * 2;
        int boxHeight = height + PADDING * 2;
        int x = switch (position) {
            case TOP_LEFT -> MARGIN;
            case TOP_RIGHT -> graphics.guiWidth() - MARGIN - boxWidth;
            case CENTER, TOP_CENTER -> graphics.guiWidth() / 2 - boxWidth / 2;
        };
        int y = switch (position) {
            case CENTER -> graphics.guiHeight() / 2 + 16;
            case TOP_CENTER -> MARGIN + 18; // below the boss bar area's first row
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
        };
        graphics.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND);
        if (card.isGrading()) {
            ThinkingAnimation.draw(graphics, font, x + PADDING, y + PADDING, card.gradingElapsedMillis(), card.modelName());
        }
        else { // graded, waiting for the minimum play time or for the player to be free
            int seconds = card.secondsUntilReturn();
            Component text = Component.literal("✔ ").withColor(CommonColors.GREEN)
                    .append(Component.literal(seconds > 0 ? "Graded, your card is back in " + seconds + "s" : "Graded, your card is coming back").withColor(CommonColors.WHITE));
            graphics.text(font, text, x + boxWidth / 2 - font.width(text) / 2, y + PADDING + (height - font.lineHeight) / 2, CommonColors.WHITE, true);
        }
    }

    /** m:ss, or h:mm:ss for long intervals. Rounds up so the timer never shows 0:00 while still counting. */
    static String formatDuration(long millis) {
        long totalSeconds = (millis + 999) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds) : String.format("%d:%02d", minutes, seconds);
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, List<Component> lines, ModConfig.HudPosition position) {
        int lineHeight = font.lineHeight + 1;
        int textWidth = 0;
        for (Component line : lines) textWidth = Math.max(textWidth, font.width(line));
        int boxWidth = textWidth + PADDING * 2;
        int boxHeight = lines.size() * lineHeight - 1 + PADDING * 2;

        boolean right = position == ModConfig.HudPosition.TOP_RIGHT || position == ModConfig.HudPosition.BOTTOM_RIGHT;
        boolean bottom = position == ModConfig.HudPosition.BOTTOM_LEFT || position == ModConfig.HudPosition.BOTTOM_RIGHT;
        int x = right ? graphics.guiWidth() - MARGIN - boxWidth : MARGIN;
        int y = bottom ? graphics.guiHeight() - MARGIN - boxHeight : MARGIN;

        graphics.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND);
        int textY = y + PADDING;
        for (Component line : lines) {
            int textX = right ? x + boxWidth - PADDING - font.width(line) : x + PADDING;
            graphics.text(font, line, textX, textY, CommonColors.WHITE, true);
            textY += lineHeight;
        }
    }
}
