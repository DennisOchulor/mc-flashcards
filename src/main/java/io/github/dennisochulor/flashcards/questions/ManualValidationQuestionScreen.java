package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;

// NOTE: There is no ManualValidationResultScreen because everything is done in this class.
public class ManualValidationQuestionScreen extends QuestionScreen {
    private final MultiLineTextWidget correctAnswerText;
    private final Button showAnswerButton;
    private final Button correctButton;
    private final Button wrongButton;

    public ManualValidationQuestionScreen(Question question) {
        super(question);
        correctAnswerText = new ScalableMultilineTextWidget(Component.literal("§n§lCorrect answer:§r\n" + question.answer()), Minecraft.getInstance().font, 65);

        correctButton = Button.builder(Component.literal("Correct").withColor(CommonColors.GREEN),button -> {
            Minecraft.getInstance().player.playSound(SoundEvents.PLAYER_LEVELUP);
            FileManager.updateStats(FileManager.getStats().incrementCorrect());
            QuestionScheduler.schedule();
            this.onClose();
            ModConfig config = FileManager.getConfig();

            // run correct commands
            switch(config.commandSelectionStrategy()) {
                case EXECUTE_ALL -> config.correctAnswerCommands().forEach(c -> Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + c));
                case RANDOMISE_ONE -> {
                    RandomSource random = Minecraft.getInstance().player.getRandom();
                    String command = config.correctAnswerCommands().get(random.nextInt(config.correctAnswerCommands().size()));
                    Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + command);
                }
                case OFF -> {}
            }
        }).build();
        wrongButton = Button.builder(Component.literal("Wrong").withColor(CommonColors.SOFT_RED),button -> {
            Minecraft.getInstance().player.playSound(SoundEvents.ANVIL_LAND);
            FileManager.updateStats(FileManager.getStats().incrementWrong());
            QuestionScheduler.schedule();
            this.onClose();
            ModConfig config = FileManager.getConfig();

            // run wrong commands
            switch(config.commandSelectionStrategy()) {
                case EXECUTE_ALL -> config.wrongAnswerCommands().forEach(c -> Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + c));
                case RANDOMISE_ONE -> {
                    RandomSource random = Minecraft.getInstance().player.getRandom();
                    String command = config.wrongAnswerCommands().get(random.nextInt(config.wrongAnswerCommands().size()));
                    Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + command);
                }
                case OFF -> {}
            }
        }).build();

        showAnswerButton = Button.builder(Component.literal("Show Answer"), button -> {
            addRenderableOnly(correctAnswerText);
            addRenderableWidget(correctButton);
            addRenderableWidget(wrongButton);
            removeWidget(button);
        }).build();
    }

    @Override
    public void init() {
        super.init();
        correctAnswerText.setWidth(175);
        correctAnswerText.setMaxWidth(175);
        correctAnswerText.setPosition(width/2 - Math.min(correctAnswerText.getWidth(), 175)/2, 150);
        correctAnswerText.setCentered(true);

        showAnswerButton.setRectangle(75,20,width/2 - 37,height - 30);
        correctButton.setRectangle(100,20,width/2 - 150,height - 30);
        wrongButton.setRectangle(100,20,width/2 + 50,height - 30);

        addRenderableWidget(showAnswerButton);
    }
}
