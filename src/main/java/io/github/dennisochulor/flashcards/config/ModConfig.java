package io.github.dennisochulor.flashcards.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record ModConfig(int interval, boolean intervalToggle) {
    public ModConfig {
        if(interval < 1) throw new IllegalArgumentException("interval must be >= 1!");
    }
}