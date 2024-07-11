package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.Utils;
import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

class QuestionEditScreen extends Screen {
    protected QuestionEditScreen(QuestionListWidget.Entry entry, String category) {
        super(Text.literal("Question Edit Screen"));
        this.questionEditBox.setText(entry.question.question());
        this.answerEditBox.setText(entry.question.answer());
        this.category = category;
        this.entry = entry;

        File file = FileManager.getImage(entry.question.imageName());
        image = entry.question.imageName() != null ? file.toPath() : null;
        if(image != null) {
            imageWidget = IconWidget.create(100,100,Utils.getImageId(file),100,100);
            imageWidget.setTooltip(Tooltip.of(Text.literal(file.getName())));
            imageButton.setMessage(Text.literal("Change Image"));
        }
        else imageWidget = null;
    }

    private String category;
    private QuestionListWidget.Entry entry;
    private final EditScreen parent = (EditScreen) MinecraftClient.getInstance().currentScreen;
    private final TextWidget title = new TextWidget(Text.literal("Edit Question"), MinecraftClient.getInstance().textRenderer);
    private final TextWidget title2 = new TextWidget(Text.literal("Question:"),MinecraftClient.getInstance().textRenderer);
    private final TextWidget title3 = new TextWidget(Text.literal("Answer:"),MinecraftClient.getInstance().textRenderer);
    private final EditBoxWidget questionEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.empty(),Text.empty());
    private final EditBoxWidget answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.empty(),Text.empty());

    private Path image;
    private IconWidget imageWidget;
    private final ButtonWidget removeButton = ButtonWidget.builder(Text.literal("Remove Image"),button -> {
        remove(imageWidget);
        imageWidget = null;
        image = null;
        remove(button);
    }).build();

    private final ButtonWidget imageButton = ButtonWidget.builder(Text.literal("Add Image"),button -> {
        Thread.startVirtualThread(() -> { // don't hang the Render thread
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose an image file");
            fileChooser.setFileFilter(Utils.FILE_NAME_EXTENSION_FILTER);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.showOpenDialog(null);
            File file = fileChooser.getSelectedFile();
            if(file == null) return;
            if(!Utils.FILE_NAME_EXTENSION_FILTER.accept(file)) {
                MinecraftClient.getInstance().execute(() -> {
                    PopupScreen popup = new PopupScreen.Builder(this,Text.literal("Encountered error with chosen file " + file.getName())).message(Text.literal("The chosen file must be one of the following file formats: " + Utils.FILE_NAME_EXTENSION_FILTER.getDescription())).button(Text.literal("Done"),PopupScreen::close).build();
                    MinecraftClient.getInstance().setScreen(popup);
                });
                return;
            }
            if(MinecraftClient.getInstance().currentScreen != this) return;

            MinecraftClient.getInstance().execute(() -> {
                image = file.toPath();
                if(imageWidget != null) remove(imageWidget);
                imageWidget = IconWidget.create(100,100,Utils.getImageId(file),100,100);
                imageWidget.setTooltip(Tooltip.of(Text.literal(file.getName())));
                imageWidget.setPosition(button.getX() - 10, button.getY() + 25);
                addDrawable(imageWidget);
                addDrawableChild(removeButton);
                button.setMessage(Text.literal("Change Image"));
            });
        });
    }).build();

    private final ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"),button -> {
        List<Question> list = parent.map.get(category);
        String imageName = null;
        if(image != null) imageName = FileManager.saveImage(image);
        Question q = new Question(questionEditBox.getText(),imageName,answerEditBox.getText());
        list.set(entry.index,q);
        parent.questionList.getSelectedOrNull().question = q;
        this.close();
    }).build();

    @Override
    public void init() {
        title.alignCenter().setDimensionsAndPosition(width,10,0,10);
        title2.alignCenter().setDimensionsAndPosition(width,10,0,50);
        title3.alignCenter().setDimensionsAndPosition(width,10,0,140);
        questionEditBox.setDimensionsAndPosition(200,50,width/2-100,70);
        questionEditBox.setMaxLength(200);
        questionEditBox.setChangeListener(text -> doneButton.active = !text.isBlank() && !answerEditBox.getText().isBlank());
        answerEditBox.setDimensionsAndPosition(200,50,width/2-100,160);
        answerEditBox.setMaxLength(100);
        answerEditBox.setChangeListener(text -> doneButton.active = !text.isBlank() && !questionEditBox.getText().isBlank());
        doneButton.setDimensionsAndPosition(100,20,width/2-50,230);
        doneButton.active = false;
        imageButton.setDimensionsAndPosition(80,20,width/2 + 140,80);
        removeButton.setDimensionsAndPosition(80,20,width/2 + 140,210);

        addDrawable(title);
        addDrawable(title2);
        addDrawableChild(questionEditBox);
        addDrawable(title3);
        addDrawableChild(answerEditBox);
        addDrawableChild(imageButton);
        if(imageWidget != null) {
            imageWidget.setPosition(imageButton.getX() - 10, imageButton.getY() + 25);
            addDrawable(imageWidget);
            addDrawableChild(removeButton);
        }
        addDrawableChild(doneButton);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

}
