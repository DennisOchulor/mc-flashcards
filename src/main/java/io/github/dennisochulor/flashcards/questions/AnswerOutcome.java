package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.anki.StudyStats;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/** Shared side effects of answering a question: sound, stats and the configured reward/penalty commands. */
public final class AnswerOutcome {
    private AnswerOutcome() {}

    public static void playSoundAndRecordStats(boolean correct) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.playSound(correct ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ANVIL_LAND);
        ModStats stats = FileManager.getStats();
        FileManager.updateStats(correct ? stats.incrementCorrect() : stats.incrementWrong());
        StudyStats.recordAnswer(correct);
    }

    public static void runCommands(boolean correct) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (player == null || connection == null) return;

        ModConfig config = FileManager.getConfig();
        List<String> commands = correct ? config.correctAnswerCommands() : config.wrongAnswerCommands();
        if (commands.isEmpty()) return;

        switch (config.commandSelectionStrategy()) {
            case EXECUTE_ALL -> commands.forEach(c -> connection.sendCommand("execute as @s at @s run " + c));
            case RANDOMISE_ONE -> connection.sendCommand("execute as @s at @s run " + commands.get(player.getRandom().nextInt(commands.size())));
            case OFF -> {}
        }
    }
}
