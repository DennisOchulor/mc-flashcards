package io.github.dennisochulor.flashcards.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.HashMap;

@Environment(EnvType.CLIENT)
public record ModConfig(int interval, boolean intervalToggle, HashMap<String,Boolean> categoryToggle) {
    public ModConfig {
        if(interval < 1) throw new IllegalArgumentException("interval must be >= 1!");
    }
}