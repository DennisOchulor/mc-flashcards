package io.github.dennisochulor.flashcards.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.HashMap;
import java.util.List;

@Environment(EnvType.CLIENT)
public record ModConfig(int interval, boolean intervalToggle, Boolean validationToggle, HashMap<String,Boolean> categoryToggle, List<String> correctAnswerCommands, List<String> wrongAnswerCommands) {
    public ModConfig {
        if(interval < 1) throw new IllegalArgumentException("interval must be >= 1!");
        if(correctAnswerCommands == null) correctAnswerCommands = List.of();
        if(wrongAnswerCommands == null) wrongAnswerCommands = List.of();
        if(validationToggle == null) validationToggle = true;

        correctAnswerCommands = correctAnswerCommands.stream().filter(s -> !s.isBlank()).map(s -> {
            if(s.startsWith("/")) s = s.substring(1);
            return s;
        }).toList();
        wrongAnswerCommands = wrongAnswerCommands.stream().filter(s -> !s.isBlank()).map(s -> {
            if(s.startsWith("/")) s = s.substring(1);
            return s;
        }).toList();
    }
}