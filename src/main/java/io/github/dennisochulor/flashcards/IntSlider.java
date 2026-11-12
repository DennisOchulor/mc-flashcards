package io.github.dennisochulor.flashcards;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.IntFunction;

/** Slider for a whole number between min and max; the label is built from the current value. */
public class IntSlider extends AbstractSliderButton {
    private final int min;
    private final int max;
    private final IntFunction<Component> label;

    public IntSlider(int width, int height, int min, int max, int value, IntFunction<Component> label) {
        super(0, 0, width, height, Component.empty(), (double) (Math.clamp(value, min, max) - min) / (max - min));
        this.min = min;
        this.max = max;
        this.label = label;
        updateMessage();
    }

    public int intValue() {
        return min + (int) Math.round(value * (max - min));
    }

    @Override
    protected void updateMessage() {
        setMessage(label.apply(intValue()));
    }

    @Override
    protected void applyValue() {
        // read with intValue() when the screen closes
    }
}
