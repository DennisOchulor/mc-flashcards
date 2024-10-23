package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

public class AutoValidationQuestionScreen extends QuestionScreen {

    private final EditBoxWidget answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.literal("Write your answer here..."),Text.empty());
    private final ButtonWidget submitButton;

    public AutoValidationQuestionScreen(Question question) {
        super(question);
        submitButton = ButtonWidget.builder(Text.literal("Submit"), button -> {
            MinecraftClient.getInstance().setScreen(new AutoValidationResultScreen(question, answerEditBox.getText()));
        }).build();
    }

    @Override
    public void init() {
        super.init();
        answerEditBox.setPosition(width/2 - answerEditBox.getWidth()/2, 175);
        answerEditBox.setMaxLength(100);
        answerEditBox.setChangeListener(answer -> submitButton.active = !answerEditBox.getText().isBlank());

        submitButton.setDimensionsAndPosition(75,20,width/2 - 37,235);
        submitButton.active = false;

        addDrawableChild(answerEditBox);
        addDrawableChild(submitButton);
    }

}