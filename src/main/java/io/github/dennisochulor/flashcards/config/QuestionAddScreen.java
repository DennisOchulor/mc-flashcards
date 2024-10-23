package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ImageUtils;
import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

class QuestionAddScreen extends Screen {
    protected QuestionAddScreen(String category) {
        super(Text.literal("Question Add Screen"));
        this.category = category;
    }

    private String category;
    private final EditScreen parent = (EditScreen) MinecraftClient.getInstance().currentScreen;
    private final TextWidget title = new TextWidget(Text.literal("Add Question"), MinecraftClient.getInstance().textRenderer);
    private final TextWidget title2 = new TextWidget(Text.literal("Question:"),MinecraftClient.getInstance().textRenderer);
    private final TextWidget title3 = new TextWidget(Text.literal("Answer:"),MinecraftClient.getInstance().textRenderer);
    private final EditBoxWidget questionEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.empty(),Text.empty());
    private final EditBoxWidget answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.empty(),Text.empty());

    private Path image = null;
    private Identifier imageId = null;
    private IconWidget imageWidget = null;
    private final ButtonWidget removeButton = ButtonWidget.builder(Text.literal("Remove Image"),button -> {
        remove(imageWidget);
        MinecraftClient.getInstance().getTextureManager().destroyTexture(imageId);
        imageId = null;
        imageWidget = null;
        image = null;
        remove(button);
    }).build();

    private final ButtonWidget imageButton = ButtonWidget.builder(Text.literal("Add Image"),button -> {
        Thread.startVirtualThread(() -> { // don't hang the Render thread
            JDialog wrapper = new JDialog((Dialog) null);
            wrapper.setAlwaysOnTop(true);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose an image file");
            fileChooser.setFileFilter(ImageUtils.FILE_NAME_EXTENSION_FILTER);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.showOpenDialog(wrapper);
            File file = fileChooser.getSelectedFile();
            if(file == null) return;
            if(!ImageUtils.FILE_NAME_EXTENSION_FILTER.accept(file)) {
                MinecraftClient.getInstance().execute(() -> {
                    PopupScreen popup = new PopupScreen.Builder(this,Text.literal("Encountered error with chosen file " + file.getName())).message(Text.literal("The chosen file must be one of the following file formats: " + ImageUtils.FILE_NAME_EXTENSION_FILTER.getDescription())).button(Text.literal("Done"),PopupScreen::close).build();
                    MinecraftClient.getInstance().setScreen(popup);
                });
                return;
            }
            if(MinecraftClient.getInstance().currentScreen != this) return;

            MinecraftClient.getInstance().execute(() -> {
                if(image != null) {
                    remove(imageWidget);
                    remove(removeButton);
                    MinecraftClient.getInstance().getTextureManager().destroyTexture(imageId);
                }

                ImageUtils.ImagePackage imgPkg = ImageUtils.getImageId(file);
                int width = (int)(100 * imgPkg.widthScaler());
                int height = (int)(100 * imgPkg.heightScaler());
                image = file.toPath();
                imageWidget = IconWidget.create(width,height,imgPkg.id(),width,height);
                imageId = imgPkg.id();

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
        list.add(q);
        QuestionListWidget.Entry e = new QuestionListWidget.Entry(q);
        parent.questionList.children().add(e);
        parent.questionList.setSelected(e);
        this.close();
    }).build();

    @Override
    public void init() {
        title.alignCenter().setDimensionsAndPosition(width,10,0,10);
        title2.alignCenter().setDimensionsAndPosition(width,10,0,50);
        title3.alignCenter().setDimensionsAndPosition(width,10,0,140);
        questionEditBox.setDimensionsAndPosition(200,50,width/2-100,70);
        questionEditBox.setMaxLength(200);
        answerEditBox.setDimensionsAndPosition(200,50,width/2-100,160);
        answerEditBox.setMaxLength(100);
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
        if(imageId != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(imageId);
    }

    @Override
    public void tick() {
        doneButton.active = !questionEditBox.getText().isBlank() && !answerEditBox.getText().isBlank();
    }

}
