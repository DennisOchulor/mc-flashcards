package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.Locale;

/**
 * "⠹ qwen2.5:7b is comparing it with the card...  4.2s" over a gliding bar.
 * Used on the card screen and on the HUD while the player keeps playing.
 */
public final class ThinkingAnimation {
    private ThinkingAnimation() {}

    private static final int GRAY = 0xFFAAAAAA;
    private static final int PURPLE = 0xFFC08CFF;
    private static final int PURPLE_CORE = 0xFFE6D2FF;
    private static final String SPINNER = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏";
    private static final String[] STEPS = {
            "is reading your answer", "is finding the key points", "is comparing it with the card",
            "is checking for mistakes", "is choosing a grade"
    };
    private static final int BAR_GAP = 5;
    private static final int BAR_HEIGHT = 3;

    /** Width that fits every step, so the animation doesn't jump around as the text changes. */
    public static int width(Font font, String model) {
        int longest = 0;
        for (String step : STEPS) longest = Math.max(longest, font.width("⠋ " + model + " " + step + "...  000.0s"));
        return longest;
    }

    public static int height(Font font) {
        return font.lineHeight + BAR_GAP + BAR_HEIGHT;
    }

    /** Draws the animation with its top-left corner at (x, y), {@link #width} wide. */
    public static void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, long elapsedMillis, String model) {
        int width = width(font, model);
        String spinner = String.valueOf(SPINNER.charAt((int) (elapsedMillis / 80 % SPINNER.length())));
        String step = STEPS[(int) Math.min(STEPS.length - 1, elapsedMillis / 1800)];
        String dots = ".".repeat((int) (elapsedMillis / 350 % 4));
        Component line = Component.literal(spinner + " ").withColor(PURPLE)
                .append(Component.literal(model + " ").withColor(CommonColors.WHITE))
                .append(Component.literal(step + dots).withColor(GRAY))
                .append(Component.literal(String.format(Locale.ROOT, "  %.1fs", elapsedMillis / 1000.0)).withColor(0xFF777777));
        graphics.text(font, line, x, y, CommonColors.WHITE, true);

        // indeterminate progress bar: a glowing segment gliding back and forth
        int barY = y + font.lineHeight + BAR_GAP;
        graphics.fill(x, barY, x + width, barY + BAR_HEIGHT, 0x40FFFFFF);
        int segment = Math.max(12, width / 4);
        double t = (Math.sin(elapsedMillis / 450.0) + 1) / 2; // 0..1..0
        int segX = x + (int) (t * (width - segment));
        graphics.fill(segX, barY, segX + segment, barY + BAR_HEIGHT, PURPLE);
        graphics.fill(segX + segment / 4, barY, segX + segment * 3 / 4, barY + BAR_HEIGHT, PURPLE_CORE);
    }
}
