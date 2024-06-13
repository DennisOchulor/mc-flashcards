package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.List;

class QuestionEditScreen extends Screen {
    protected QuestionEditScreen(QuestionListWidget.Entry entry, String category) {
        super(Text.literal("Question Edit Screen"));
        this.questionEditBox.setText(entry.question.question());
        this.answerEditBox.setText(entry.question.answer());
        this.category = category;
        this.entry = entry;
    }

    private String category;
    private QuestionListWidget.Entry entry;
    private final EditScreen parent = (EditScreen) MinecraftClient.getInstance().currentScreen;
    private final TextWidget title = new TextWidget(Text.literal("Edit Question"), MinecraftClient.getInstance().textRenderer);
    private final TextWidget title2 = new TextWidget(Text.literal("Question:"),MinecraftClient.getInstance().textRenderer);
    private final TextWidget title3 = new TextWidget(Text.literal("Answer:"),MinecraftClient.getInstance().textRenderer);
    private final EditBoxWidget questionEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.empty(),Text.empty());
    private final EditBoxWidget answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.empty(),Text.empty());
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"),button -> {
        List<Question> list = parent.map.get(category);
        Question q = new Question(questionEditBox.getText(),answerEditBox.getText());
        list.set(entry.index,q);
        parent.questionList.getSelectedOrNull().question = q;
        this.close();
    }).build();

    @Override
    public void init() {
        title.alignCenter().setDimensionsAndPosition(150,30,width/2-68,10);
        title2.alignCenter().setDimensionsAndPosition(100,15,width/2-45,50);
        title3.alignCenter().setDimensionsAndPosition(100,15,width/2-45,140);
        questionEditBox.setDimensionsAndPosition(200,50,width/2-95,70);
        questionEditBox.setMaxLength(200);
        questionEditBox.setChangeListener(text -> doneButton.active = !text.isBlank() && !answerEditBox.getText().isBlank());
        answerEditBox.setDimensionsAndPosition(200,50,width/2-95,160);
        answerEditBox.setMaxLength(100);
        answerEditBox.setChangeListener(text -> doneButton.active = !text.isBlank() && !questionEditBox.getText().isBlank());
        doneButton.setDimensionsAndPosition(100,20,width/2-47,230);
        doneButton.active = false;

        addDrawable(title);
        addDrawable(title2);
        addDrawableChild(questionEditBox);
        addDrawable(title3);
        addDrawableChild(answerEditBox);
        addDrawableChild(doneButton);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

}
