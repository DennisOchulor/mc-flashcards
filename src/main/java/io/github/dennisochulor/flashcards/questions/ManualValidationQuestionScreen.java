package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

// NOTE: There is no ManualValidationResultScreen because everything is done in this class.
public class ManualValidationQuestionScreen extends QuestionScreen {
    private final ScalableMultilineTextWidget correctAnswerText;
    private final Button showAnswerButton;
    private final Button correctButton;
    private final Button wrongButton;

    public ManualValidationQuestionScreen(Question question) {
        super(question);
        correctAnswerText = new ScalableMultilineTextWidget(Component.literal("§n§lCorrect answer:§r\n" + question.answer()), Minecraft.getInstance().font, 65);
        correctAnswerText.setCentered(true).setMaxWidth(175);

        correctButton = Button.builder(Component.literal("Correct").withColor(CommonColors.GREEN),_ -> answer(true)).build();
        wrongButton = Button.builder(Component.literal("Wrong").withColor(CommonColors.SOFT_RED),_ -> answer(false)).build();

        showAnswerButton = Button.builder(Component.literal("Show Answer"), button -> {
            addRenderableOnly(correctAnswerText);
            addRenderableWidget(correctButton);
            addRenderableWidget(wrongButton);
            removeWidget(button);
        }).build();
    }

    private void answer(boolean correct) {
        AnswerOutcome.playSoundAndRecordStats(correct);
        QuestionScheduler.schedule();
        this.onClose();
        AnswerOutcome.runCommands(correct);
    }

    @Override
    public void init() {
        super.init();
        correctAnswerText.setMaxHeigth((int) (height * 0.25));
        correctAnswerText.setPosition(width/2 - correctAnswerText.getWidth()/2, (int) (height * 0.6));

        showAnswerButton.setRectangle(75,20,width/2 - 37,height - 30);
        correctButton.setRectangle(100,20,width/2 - 150,height - 30);
        wrongButton.setRectangle(100,20,width/2 + 50,height - 30);

        addRenderableWidget(showAnswerButton);
    }
}
