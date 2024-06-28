package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

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

    //to make the MultilineTextWidget center the text proprely, oh dear...
    public static final String offset = "\n                                                                ";

    @Override
    public void init() {
        titleText = new TextWidget(Text.literal("Answer this question"), MinecraftClient.getInstance().textRenderer);
        titleText.setPosition(width/2 - 45, 15);

        questionText = new MultilineTextWidget(Text.literal(question.question() + offset), MinecraftClient.getInstance().textRenderer);
        questionText.setPosition(width/2 - 120, 45);
        questionText.setMaxWidth(250);
        questionText.setCentered(true);

        answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.literal("Write your answer here..."),Text.empty());
        answerEditBox.setPosition(width/2 - 100, 120);
        answerEditBox.setMaxLength(100);
        answerEditBox.setChangeListener(answer -> submitButton.active = !answerEditBox.getText().isBlank());

        submitButton = ButtonWidget.builder(Text.literal("Submit"), button -> {
            MinecraftClient.getInstance().setScreen(new ResultScreen(question, answerEditBox.getText()));
        }).dimensions(width/2 - 35,200,75,20).build();
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
