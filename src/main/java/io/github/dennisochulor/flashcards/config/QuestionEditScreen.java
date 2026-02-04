package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ImageUtils;
import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

class QuestionEditScreen extends Screen {
    protected QuestionEditScreen(QuestionListWidget.Entry entry, String category) {
        super(Component.literal("Question Edit Screen"));
        this.questionEditBox.setValue(entry.question.question());
        this.answerEditBox.setValue(entry.question.answer());
        this.category = category;
        this.entry = entry;

        if (entry.question.imageName() != null) {
            File file = FileManager.getImageFile(entry.question.imageName());
            image = file.toPath();
            ImageUtils.ImagePackage imgPkg = ImageUtils.getImagePackage(file);

            if (imgPkg == null) {
                imageWidget = ImageWidget.texture(100,100, Identifier.withDefaultNamespace("textures/missing.png"),100,100);
                imageWidget.setTooltip(Tooltip.create(Component.literal(file.getName() + " seems to be missing...")));
            }
            else {
                imageId = imgPkg.id();
                int width = (int)(100 * imgPkg.widthScaler());
                int height = (int)(100 * imgPkg.heightScaler());
                imageWidget = ImageWidget.texture(width,height,imgPkg.id(),width,height);
                imageWidget.setTooltip(Tooltip.create(Component.literal(file.getName())));
            }
            imageButton.setMessage(Component.literal("Change Image"));
        }
    }

    private String category;
    private QuestionListWidget.Entry entry;
    private final EditScreen parent = (EditScreen) Objects.requireNonNull(Minecraft.getInstance().screen);
    private final StringWidget title = new StringWidget(Component.literal("Edit Question"), Minecraft.getInstance().font);
    private final StringWidget title2 = new StringWidget(Component.literal("Question:"),Minecraft.getInstance().font);
    private final StringWidget title3 = new StringWidget(Component.literal("Answer:"),Minecraft.getInstance().font);
    private final MultiLineEditBox questionEditBox = MultiLineEditBox.builder().build(Minecraft.getInstance().font, 200, 50, Component.empty());
    private final MultiLineEditBox answerEditBox = MultiLineEditBox.builder().build(Minecraft.getInstance().font, 200, 50, Component.empty());

    @Nullable
    private Path image;
    @Nullable
    private ImageWidget imageWidget;
    @Nullable
    private Identifier imageId;
    private final Button removeButton = Button.builder(Component.literal("Remove Image"),button -> {
        removeWidget(imageWidget);
        Minecraft.getInstance().getTextureManager().release(imageId);
        imageId = null;
        imageWidget = null;
        image = null;
        removeWidget(button);
    }).build();

    private final Button imageButton = Button.builder(Component.literal("Add Image"),button -> {
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
            if (file == null) return;
            if (!ImageUtils.FILE_NAME_EXTENSION_FILTER.accept(file)) {
                Minecraft.getInstance().execute(() -> {
                    PopupScreen popup = new PopupScreen.Builder(this,Component.literal("Encountered error with chosen file " + file.getName()))
                            .addMessage(Component.literal("The chosen file must be one of the following file formats: " + ImageUtils.FILE_NAME_EXTENSION_FILTER.getDescription()))
                            .addButton(Component.literal("Done"),PopupScreen::onClose).build();
                    Minecraft.getInstance().setScreen(popup);
                });
                return;
            }
            if (Minecraft.getInstance().screen != this) return;

            Minecraft.getInstance().execute(() -> {
                if (image != null) {
                    removeWidget(imageWidget);
                    removeWidget(removeButton);
                    Minecraft.getInstance().getTextureManager().release(imageId);
                }

                ImageUtils.ImagePackage imgPkg = Objects.requireNonNull(ImageUtils.getImagePackage(file));
                int width = (int)(100 * imgPkg.widthScaler());
                int height = (int)(100 * imgPkg.heightScaler());
                image = file.toPath();
                imageWidget = ImageWidget.texture(width,height,imgPkg.id(),width,height);
                imageId = imgPkg.id();

                imageWidget.setTooltip(Tooltip.create(Component.literal(file.getName())));
                imageWidget.setPosition(button.getX() - 10, button.getY() + 25);
                addRenderableOnly(imageWidget);
                addRenderableWidget(removeButton);
                button.setMessage(Component.literal("Change Image"));
            });
        });
    }).build();

    private final Button doneButton = Button.builder(Component.literal("Done"),_ -> {
        List<Question> list = parent.categoriesMap.get(category);
        String imageName = null;
        if (image != null) imageName = FileManager.saveImage(image);

        Question q = new Question(questionEditBox.getValue(),imageName,answerEditBox.getValue());
        list.set(list.indexOf(entry.question),q);
        Objects.requireNonNull(parent.questionList.getSelected()).question = q;
        this.onClose();
    }).build();

    @Override
    public void init() {
        title.setPosition(width/2 - title.getWidth()/2,10);
        title2.setPosition(width/2 - title2.getWidth()/2,50);
        title3.setPosition(width/2 - title3.getWidth()/2,140);
        questionEditBox.setRectangle(200,50,width/2-100,70);
        questionEditBox.setCharacterLimit(500);
        answerEditBox.setRectangle(200,50,width/2-100,160);
        answerEditBox.setCharacterLimit(300);
        doneButton.setRectangle(100,20,width/2-50,height - 30);
        doneButton.active = false;
        imageButton.setRectangle(80,20,width/2 + 140,80);
        removeButton.setRectangle(80,20,width/2 + 140,210);

        addRenderableOnly(title);
        addRenderableOnly(title2);
        addRenderableWidget(questionEditBox);
        addRenderableOnly(title3);
        addRenderableWidget(answerEditBox);
        addRenderableWidget(imageButton);
        if (imageWidget != null) {
            imageWidget.setPosition(imageButton.getX() - 10, imageButton.getY() + 25);
            addRenderableOnly(imageWidget);
            addRenderableWidget(removeButton);
        }
        addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
        if (imageId != null) Minecraft.getInstance().getTextureManager().release(imageId);
    }

    @Override
    public void tick() {
        doneButton.active = !questionEditBox.getValue().isBlank() && !answerEditBox.getValue().isBlank();
    }

}
