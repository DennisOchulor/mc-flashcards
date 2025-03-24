package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.random.Random;

// NOTE: There is no ManualValidationResultScreen because everything is done in this class.
public class ManualValidationQuestionScreen extends QuestionScreen {
    private final MultilineTextWidget correctAnswerText;
    private final ButtonWidget showAnswerButton;
    private final ButtonWidget correctButton;
    private final ButtonWidget wrongButton;

    public ManualValidationQuestionScreen(Question question) {
        super(question);
        correctAnswerText = new ScalableMultilineTextWidget(Text.literal("§n§lCorrect answer:§r\n" + question.answer()), MinecraftClient.getInstance().textRenderer, 65);

        correctButton = ButtonWidget.builder(Text.literal("Correct").withColor(Colors.GREEN),button -> {
            MinecraftClient.getInstance().player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER,1,1);
            FileManager.updateStats(FileManager.getStats().incrementCorrect());
            QuestionScheduler.schedule();
            this.close();
            ModConfig config = FileManager.getConfig();

            // run correct commands if applicable
            if(MinecraftClient.getInstance().isIntegratedServerRunning() || MinecraftClient.getInstance().player.hasPermissionLevel(2)) {
                switch(config.commandSelectionStrategy()) {
                    case EXECUTE_ALL -> config.correctAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + c));
                    case RANDOMISE_ONE -> {
                        Random random = MinecraftClient.getInstance().player.getRandom();
                        String command = config.correctAnswerCommands().get(random.nextInt(config.correctAnswerCommands().size()));
                        MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + command);
                    }
                }
            }
        }).build();
        wrongButton = ButtonWidget.builder(Text.literal("Wrong").withColor(Colors.LIGHT_RED),button -> {
            MinecraftClient.getInstance().player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.MASTER,1,1);
            FileManager.updateStats(FileManager.getStats().incrementWrong());
            QuestionScheduler.schedule();
            this.close();
            ModConfig config = FileManager.getConfig();

            // run wrong commands if applicable
            if(MinecraftClient.getInstance().isIntegratedServerRunning() || MinecraftClient.getInstance().player.hasPermissionLevel(2)) {
                switch(config.commandSelectionStrategy()) {
                    case EXECUTE_ALL -> config.wrongAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + c));
                    case RANDOMISE_ONE -> {
                        Random random = MinecraftClient.getInstance().player.getRandom();
                        String command = config.wrongAnswerCommands().get(random.nextInt(config.wrongAnswerCommands().size()));
                        MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + command);
                    }
                }
            }
        }).build();

        showAnswerButton = ButtonWidget.builder(Text.literal("Show Answer"), button -> {
            addDrawable(correctAnswerText);
            addDrawableChild(correctButton);
            addDrawableChild(wrongButton);
            remove(button);
        }).build();
    }

    @Override
    public void init() {
        super.init();
        correctAnswerText.setWidth(175);
        correctAnswerText.setMaxWidth(175);
        correctAnswerText.setPosition(width/2 - Math.min(correctAnswerText.getWidth(), 175)/2, 150);
        correctAnswerText.setCentered(true);

        showAnswerButton.setDimensionsAndPosition(75,20,width/2 - 37,215);
        correctButton.setDimensionsAndPosition(100,20,width/2 - 150,215);
        wrongButton.setDimensionsAndPosition(100,20,width/2 + 50,215);

        addDrawableChild(showAnswerButton);
    }
}
