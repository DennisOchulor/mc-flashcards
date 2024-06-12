package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class QuestionScreen extends Screen {

    public QuestionScreen(Question question) {
        super(Text.literal("Question Prompt"));
        this.question = question;
    }

    private TextWidget titleText;
    private TextFieldWidget answerTextField;
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

        MutableText text = Text.empty();
        answerTextField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 300, 15,text);
        answerTextField.setPosition(width/2 - 150, 100);
        answerTextField.setMaxLength(100);
        answerTextField.setPlaceholder(Text.literal("Write your answer here."));
        answerTextField.setChangedListener(answer -> submitButton.active = !answerTextField.getText().isBlank());

        submitButton = ButtonWidget.builder(Text.literal("Submit"), button -> {
            MinecraftClient.getInstance().setScreen(new ResultScreen(question,answerTextField.getText()));
        }).dimensions(width/2 - 35,200,75,20).build();
        submitButton.active = false;

        addDrawable(titleText);
        addDrawable(questionText);
        addDrawableChild(answerTextField);
        addDrawableChild(submitButton);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
