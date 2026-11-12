package io.github.dennisochulor.flashcards;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/** Slider for a 0.00-1.00 score, shown as "{label}0.73". Values snap to hundredths. */
public class ScoreSlider extends AbstractSliderButton {
    private final String label;
    private final DoubleConsumer onChange;

    public ScoreSlider(int width, int height, String label, double value, DoubleConsumer onChange) {
        super(0, 0, width, height, Component.empty(), round(value));
        this.label = label;
        this.onChange = onChange;
        updateMessage();
    }

    private static double round(double value) {
        return Math.round(Math.min(1, Math.max(0, value)) * 100) / 100.0;
    }

    public double score() {
        return round(value);
    }

    /** Sets the value without notifying the listener (e.g. when another slider pushes this one). */
    public void setScoreSilently(double score) {
        value = round(score);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + String.format(Locale.ROOT, "%.2f", score())));
    }

    @Override
    protected void applyValue() {
        onChange.accept(score());
    }
}
