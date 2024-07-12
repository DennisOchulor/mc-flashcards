package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.ClientModInit;
import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;

public class QuestionScreen extends Screen {

    private final TextWidget titleText = new TextWidget(Text.literal("Answer this question"), MinecraftClient.getInstance().textRenderer);;
    private final EditBoxWidget answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.literal("Write your answer here..."),Text.empty());
    private final ButtonWidget submitButton;
    private final MultilineTextWidget questionText;
    private final IconWidget imageWidget;

    public QuestionScreen(Question question) {
        super(Text.literal("Question Prompt"));
        questionText = new MultilineTextWidget(Text.literal(question.question()), MinecraftClient.getInstance().textRenderer);
        submitButton = ButtonWidget.builder(Text.literal("Submit"), button -> MinecraftClient.getInstance().setScreen(new ResultScreen(question, answerEditBox.getText()))).build();

        if(question.imageName() != null) {
            Identifier id = Utils.getImageId(FileManager.getImage(question.imageName()));
            if(id == null) {
                imageWidget = IconWidget.create(140,140,Identifier.ofVanilla("textures/missing.png"),140,140);
                imageWidget.setTooltip(Tooltip.of(Text.literal(question.imageName() + " seems to be missing...")));
            }
            else imageWidget = IconWidget.create(140,140,id,140,140);
        }
        else imageWidget = null;
    }

    @Override
    public void init() {
        titleText.alignCenter().setDimensionsAndPosition(width,10,0,15);

        questionText.setWidth(250);
        questionText.setMaxWidth(250);
        questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2, 45);
        questionText.setCentered(true);

        answerEditBox.setPosition(width/2 - answerEditBox.getWidth()/2, 175); //120
        answerEditBox.setMaxLength(100);
        answerEditBox.setChangeListener(answer -> submitButton.active = !answerEditBox.getText().isBlank());

        submitButton.setDimensionsAndPosition(75,20,width/2 - 37,235); //200
        submitButton.active = false;

        addDrawable(titleText);
        addDrawable(questionText);
        addDrawableChild(answerEditBox);
        addDrawableChild(submitButton);
        if(imageWidget != null) {
            addDrawable(imageWidget);
            questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2 + 100, 45);
            imageWidget.setPosition(width/4 - 75,25);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
