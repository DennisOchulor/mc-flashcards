package io.github.dennisochulor.flashcards.questions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
class ResultScreen extends Screen {

    protected ResultScreen(Question question, String userAnswer) {
        super(Text.literal("Question Result"));
        this.question = question;
        this.isCorrect = question.answer().equalsIgnoreCase(userAnswer);
        this.userAnswer = userAnswer;
    }

    private TextWidget titleText;
    private TextWidget resultText;
    private ButtonWidget doneButton;
    private MultilineTextWidget questionText;
    private MultilineTextWidget yourAnswerText;
    private MultilineTextWidget correctAnswerText;
    private final Question question;
    private final boolean isCorrect;
    private final String userAnswer;

    @Override
    public void init() {
        titleText = new TextWidget(Text.literal("Your answer is "), MinecraftClient.getInstance().textRenderer);
        titleText.setPosition(width/2 - 50, 15);

        resultText = new TextWidget(Text.literal(isCorrect ? "CORRECT" : "WRONG"), MinecraftClient.getInstance().textRenderer);
        if(isCorrect) resultText.setTextColor(9498256); //green
        else resultText.setTextColor(16711680); //red
        resultText.setPosition(width/2 + 30, 15);

        questionText = new MultilineTextWidget(Text.literal("Question:\n" + question.question() + QuestionScreen.offset), MinecraftClient.getInstance().textRenderer);
        questionText.setPosition(width/2 - 120, 45);
        questionText.setMaxWidth(250);
        questionText.setCentered(true);

        yourAnswerText = new MultilineTextWidget(Text.literal("Your answer:\n" + userAnswer + QuestionScreen.offset), MinecraftClient.getInstance().textRenderer);
        yourAnswerText.setDimensionsAndPosition(250,50,width/2 - 120, 130);
        yourAnswerText.setMaxWidth(250);
        yourAnswerText.setCentered(true);

        if(!isCorrect) {
            correctAnswerText = new MultilineTextWidget(Text.literal("Correct answer:\n" + question.answer() + QuestionScreen.offset), MinecraftClient.getInstance().textRenderer);
            correctAnswerText.setDimensionsAndPosition(250,50,width/2 - 120, 180);
            correctAnswerText.setMaxWidth(250);
            correctAnswerText.setCentered(true);
            addDrawable(correctAnswerText);
        }

        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            QuestionScheduler.schedule();
            this.close();
        }).dimensions(width/2 - 35,220,75,20).build();

        addDrawable(titleText);
        addDrawable(resultText);
        addDrawable(questionText);
        addDrawableChild(doneButton);
        addDrawable(yourAnswerText);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
