package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.ClickableImageWidget;
import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ImageUtils;
import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

class QuestionEditScreen extends Screen {
    private final EditScreen parent = (EditScreen) Objects.requireNonNull(Minecraft.getInstance().gui.screen());
    private final StringWidget title;
    private final StringWidget questionTitle = new StringWidget(Component.literal("Question:"),Minecraft.getInstance().font);
    private final StringWidget answerTitle = new StringWidget(Component.literal("Answer:"),Minecraft.getInstance().font);
    private MultiLineEditBox questionEditBox = MultiLineEditBox.builder().build(Minecraft.getInstance().font, 200, 50, Component.empty());
    private MultiLineEditBox answerEditBox = MultiLineEditBox.builder().build(Minecraft.getInstance().font, 200, 50, Component.empty());
    private final Button doneButton;

    @Nullable private File imageFile = null;
    @Nullable private Identifier imageId = null;
    private ImageWidget imageWidget; // ignore null warning, it is initialized during init()

    private final Button removeButton = SpriteIconButton.builder(Component.literal("Remove Image"), _ -> {
        Minecraft.getInstance().getTextureManager().release(imageId);
        imageId = null;
        imageFile = null;
        setImageWidget(true);
    }, true).withTootip().sprite(new WidgetSprites(Identifier.withDefaultNamespace("widget/cross_button"),
                    Identifier.withDefaultNamespace("widget/cross_button_highlighted")), 20, 20).size(20, 20).build();

    private QuestionEditScreen(@Nullable Question question, DoneButtonAction doneButtonAction) {
        Component titleText = Component.literal(question == null ? "Add a new question" : "Edit question");

        super(titleText);

        this.title = new StringWidget(titleText, Minecraft.getInstance().font);
        this.doneButton = Button.builder(Component.literal("Done"),_ -> {
            doneButtonAction.onDoneButtonPressed(parent, questionEditBox.getValue(), answerEditBox.getValue(), imageFile);
            this.onClose();
        }).size(100, 20).build();

        if (question != null) {
            questionEditBox.setValue(question.question());
            answerEditBox.setValue(question.answer());

            if (question.imageName() != null) {
                imageFile = FileManager.getImageFile(question.imageName());
                ImageUtils.ImagePackage imgPkg = ImageUtils.getImagePackage(imageFile);

                if (imgPkg == ImageUtils.MISSING_TEXTURE) {
                    imageFile = null;
                }
                else {
                    imageId = imgPkg.id();
                }
            }
        }

        removeButton.setTooltipDelay(Duration.ofSeconds(2));
    }

    @Override
    public void init() {
        // have to recreate editbox everytime since its internal textfield cannot resize its width
        int partWidth = (int) (width * 0.45);
        int partHeight = (int) (height * 0.25);
        String prevQuesValue = questionEditBox.getValue();
        questionEditBox = MultiLineEditBox.builder().build(Minecraft.getInstance().font, partWidth, partHeight, Component.literal("Question Textbox"));
        questionEditBox.setCharacterLimit(500);
        questionEditBox.setValue(prevQuesValue);
        String prevAnsValue = answerEditBox.getValue();
        answerEditBox = MultiLineEditBox.builder().build(Minecraft.getInstance().font, partWidth, partHeight, Component.literal("Answer Textbox"));
        answerEditBox.setCharacterLimit(300);
        answerEditBox.setValue(prevAnsValue);

        LinearLayout textEditLayout = LinearLayout.vertical().spacing(10);
        textEditLayout.defaultCellSetting().alignHorizontallyCenter();
        textEditLayout.addChild(questionTitle);
        textEditLayout.addChild(questionEditBox, textEditLayout.newCellSettings().paddingBottom(5));
        textEditLayout.addChild(answerTitle);
        textEditLayout.addChild(answerEditBox);

        LinearLayout imageEditLayout = LinearLayout.vertical().spacing(10);
        imageEditLayout.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        setImageWidget(false); // false so we don't infiniloop lol
        imageEditLayout.addChild(imageWidget);
        imageEditLayout.addChild(removeButton);

        LinearLayout contents = LinearLayout.horizontal().spacing(50);
        contents.defaultCellSetting().alignHorizontallyCenter();
        contents.addChild(textEditLayout);
        contents.addChild(imageEditLayout, contents.newCellSettings().alignVerticallyMiddle());

        HeaderAndFooterLayout root = new HeaderAndFooterLayout(this, 20, 30);
        root.addToHeader(title);
        root.addToContents(contents);
        root.addToFooter(doneButton);

        root.arrangeElements();
        FrameLayout.alignInRectangle(root, 0, 0, this.width, this.height, 0.5F, 0.1F);
        root.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
        if (imageId != null) Minecraft.getInstance().getTextureManager().release(imageId);
    }

    @Override
    public void tick() {
        doneButton.active = !questionEditBox.getValue().isBlank() && !answerEditBox.getValue().isBlank();
        removeButton.visible = imageFile != null;
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (!files.isEmpty()) {
            tryAttachImage(files.getFirst().toFile());
        }
    }

    private void chooseImage(MouseButtonEvent event, boolean doubleClick) {
        Thread.ofPlatform().start(() -> { // don't hang the Render thread
            JDialog wrapper = new JDialog((Dialog) null);
            wrapper.setAlwaysOnTop(true);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose an image file");
            fileChooser.setFileFilter(ImageUtils.FILE_NAME_EXTENSION_FILTER);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.showOpenDialog(wrapper);

            File file = fileChooser.getSelectedFile();
            if (file != null) tryAttachImage(file);
            wrapper.dispose();
        });
    }

    private void tryAttachImage(File file) {
        if (!ImageUtils.FILE_NAME_EXTENSION_FILTER.accept(file)) {
            Minecraft.getInstance().execute(() -> {
                PopupScreen popup = new PopupScreen.Builder(this,Component.literal("Encountered error with chosen file " + file.getName()))
                        .addMessage(Component.literal("The chosen file must be one of the following file formats: " + ImageUtils.FILE_NAME_EXTENSION_FILTER.getDescription()))
                        .addButton(Component.literal("Done"),PopupScreen::onClose).build();
                Minecraft.getInstance().gui.setScreen(popup);
            });
            return;
        }
        if (Minecraft.getInstance().gui.screen() != this) return;

        Minecraft.getInstance().execute(() -> {
            if (imageId != null) {
                Minecraft.getInstance().getTextureManager().release(imageId);
            }

            ImageUtils.ImagePackage imgPkg = ImageUtils.getImagePackage(file);
            this.imageFile = file;
            imageId = imgPkg.id();
            setImageWidget(true);
        });
    }

    // calling init() again via rebuilding needed to refresh the imageWidget instance used in the Layouts
    private void setImageWidget(boolean shouldReInit) {
        if (imageFile != null) {
            imageWidget = new ClickableImageWidget(ImageUtils.getImageWidget(imageFile, (int) (height * 0.25)), this::chooseImage);
        }
        else {
            imageWidget = new ClickableImageWidget(ImageUtils.getImageWidget(FileManager.ADD_IMAGE_IMAGE, (int) (height * 0.25)), this::chooseImage);
            imageWidget.setTooltip(Tooltip.create(Component.literal("Click to choose image file OR drag and drop an image file")));
        }

        if (shouldReInit) rebuildWidgets();
    }


    @FunctionalInterface
    private interface DoneButtonAction {
        void onDoneButtonPressed(EditScreen parent, String question, String answer, @Nullable File imageFile);
    }

    public static QuestionEditScreen newQuestion(String category) {
        return new QuestionEditScreen(null, (parent, question, answer, imageFile) -> {
            List<Question> list = parent.categoriesMap.get(category);
            String imageName = null;
            if (imageFile != null) imageName = FileManager.saveImage(imageFile.toPath());

            Question q = new Question(question, imageName, answer);
            list.add(q);
            QuestionListWidget.Entry e = new QuestionListWidget.Entry(q);
            parent.questionList.add(e);
            parent.questionList.setSelected(e);
        });
    }

    public static QuestionEditScreen fromExistingQuestion(String category, QuestionListWidget.Entry entry) {
        return new QuestionEditScreen(entry.question, (parent, question, answer, imageFile) -> {
            List<Question> list = parent.categoriesMap.get(category);
            String imageName = null;
            if (imageFile != null) imageName = FileManager.saveImage(imageFile.toPath());

            Question q = new Question(question, imageName, answer);
            list.set(list.indexOf(entry.question), q);
            Objects.requireNonNull(parent.questionList.getSelected()).question = q;
        });
    }
}
