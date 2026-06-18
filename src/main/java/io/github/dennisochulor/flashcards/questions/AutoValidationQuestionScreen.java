package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

public class AutoValidationQuestionScreen extends QuestionScreen {

    private MultiLineEditBox answerEditBox = MultiLineEditBox.builder().setPlaceholder(Component.literal("Write your answer here..."))
            .build(Minecraft.getInstance().font, 200, 50, Component.literal("Answer textbox"));
    private final Button submitButton;

    public AutoValidationQuestionScreen(Question question) {
        super(question);
        submitButton = Button.builder(Component.literal("Submit"), _ -> {
            Minecraft.getInstance().gui.setScreen(new AutoValidationResultScreen(question, answerEditBox.getValue()));
        }).build();
    }

    @Override
    public void init() {
        super.init();

        // have to recreate editbox everytime since its internal textfield cannot resize its width
        int partWidth = (int) (width * 0.5);
        int partHeight = (int) (height * 0.25);
        String prevAnsValue = answerEditBox.getValue();

        answerEditBox = MultiLineEditBox.builder().setPlaceholder(Component.literal("Write your answer here..."))
                .build(Minecraft.getInstance().font, partWidth, partHeight, Component.literal("Answer textbox"));
        answerEditBox.setPosition(width/2 - answerEditBox.getWidth()/2, height - 30 - answerEditBox.getHeight() - 10);
        answerEditBox.setCharacterLimit(300);
        answerEditBox.setValueListener(value -> submitButton.active = !value.isBlank());
        answerEditBox.setValue(prevAnsValue);

        submitButton.setRectangle(75,20,width/2 - 37,height - 30);
        submitButton.active = !answerEditBox.getValue().isBlank();

        addRenderableWidget(answerEditBox);
        addRenderableWidget(submitButton);
    }

}
