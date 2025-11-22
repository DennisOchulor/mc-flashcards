package io.github.dennisochulor.flashcards.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.List;

@Environment(EnvType.CLIENT)
public record ModConfig(int interval, boolean intervalToggle, Boolean validationToggle,
                        HashMap<String,Boolean> categoryToggle, List<String> correctAnswerCommands,
                        List<String> wrongAnswerCommands, CommandSelectionStrategy commandSelectionStrategy)
{
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

    public ModConfig {
        if(interval < 1) throw new IllegalArgumentException("interval must be >= 1!");
        if(correctAnswerCommands == null) correctAnswerCommands = List.of();
        if(wrongAnswerCommands == null) wrongAnswerCommands = List.of();
        if(validationToggle == null) validationToggle = true;
        if(commandSelectionStrategy == null) commandSelectionStrategy = CommandSelectionStrategy.EXECUTE_ALL;

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