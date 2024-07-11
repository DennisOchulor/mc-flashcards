package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import javax.swing.JFileChooser;
import java.io.File;

public class QuestionScreen extends Screen {

    public QuestionScreen(Question question) {
        super(Text.literal("Question Prompt"));
        this.question = question;
    }

    private TextWidget titleText;
    private EditBoxWidget answerEditBox;
    private ButtonWidget submitButton;
    private MultilineTextWidget questionText;
    private final Question question;

    @Override
    public void init() {
        titleText = new TextWidget(Text.literal("Answer this question"), MinecraftClient.getInstance().textRenderer);
        titleText.alignCenter().setDimensionsAndPosition(width,10,0,15);

        questionText = new MultilineTextWidget(Text.literal(question.question()), MinecraftClient.getInstance().textRenderer);
        questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2, 45);
        questionText.setMaxWidth(250);
        questionText.setCentered(true);

        answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.literal("Write your answer here..."),Text.empty());
        answerEditBox.setPosition(width/2 - answerEditBox.getWidth()/2, 120);
        answerEditBox.setMaxLength(100);
        answerEditBox.setChangeListener(answer -> submitButton.active = !answerEditBox.getText().isBlank());

        submitButton = ButtonWidget.builder(Text.literal("Submit"), button -> {
            MinecraftClient.getInstance().setScreen(new ResultScreen(question, answerEditBox.getText()));
        }).dimensions(width/2 - 37,200,75,20).build();
        submitButton.active = false;

        addDrawable(titleText);
        addDrawable(questionText);
        addDrawableChild(answerEditBox);
        addDrawableChild(submitButton);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
