package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.widget.*;
import net.minecraft.network.chat.Component;

public class AutoValidationQuestionScreen extends QuestionScreen {

    private final MultiLineEditBox answerEditBox = MultiLineEditBox.builder().setPlaceholder(Component.literal("Write your answer here...")).build(Minecraft.getInstance().font, 200, 50, Component.empty());
    private final Button submitButton;

    public AutoValidationQuestionScreen(Question question) {
        super(question);
        submitButton = Button.builder(Component.literal("Submit"), button -> {
            Minecraft.getInstance().setScreen(new AutoValidationResultScreen(question, answerEditBox.getValue()));
        }).build();
    }

    @Override
    public void init() {
        super.init();
        answerEditBox.setPosition(width/2 - answerEditBox.getWidth()/2, height - 30 - answerEditBox.getHeight() - 20);
        answerEditBox.setCharacterLimit(300);
        answerEditBox.setValueListener(answer -> submitButton.active = !answerEditBox.getValue().isBlank());

        submitButton.setRectangle(75,20,width/2 - 37,height - 30);
        submitButton.active = false;

        addRenderableWidget(answerEditBox);
        addRenderableWidget(submitButton);
    }

}
