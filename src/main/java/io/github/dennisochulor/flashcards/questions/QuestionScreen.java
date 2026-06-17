package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.ClickableImageWidget;
import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public class QuestionScreen extends Screen {
    private static final int QUESTION_TEXT_WIDTH = 250;

    private final StringWidget titleText = new StringWidget(Component.literal("Answer this question"), Minecraft.getInstance().font);
    private final MultiLineTextWidget questionText;
    @Nullable private final File imageFile;

    public QuestionScreen(Question question) {
        super(Component.literal("Question Prompt"));
        this.imageFile = question.imageName() != null ? FileManager.getImageFile(question.imageName()) : null;
        questionText = new ScalableMultilineTextWidget(Component.literal(question.question()), Minecraft.getInstance().font, 100);
        questionText.setCentered(true).setMaxWidth(QUESTION_TEXT_WIDTH);
    }

    @Override
    public void init() {
        int partHeight = (int) (0.3 * this.height);

        LinearLayout questionLayout = LinearLayout.horizontal().spacing(20);
        questionLayout.defaultCellSetting().alignHorizontallyCenter();
        if (imageFile != null) {
            ClickableImageWidget imageWidget = new ClickableImageWidget(
                    ImageUtils.getImageWidget(imageFile, partHeight), this::openLargeImageScreen);
            imageWidget.setTooltip(Tooltip.create(Component.literal("Click to enlarge")));
            questionLayout.addChild(imageWidget);
        }
        questionLayout.addChild(questionText);

        LinearLayout root = LinearLayout.vertical().spacing(20);
        root.defaultCellSetting().alignHorizontallyCenter();
        root.addChild(titleText);
        root.addChild(questionLayout);

        root.arrangeElements();
        FrameLayout.alignInRectangle(root, 0, 0, this.width, this.height, 0.5F, 0.1F);
        root.visitWidgets(this::addRenderableWidget);

    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void openLargeImageScreen(MouseButtonEvent event, boolean doubleClick) {
        Objects.requireNonNull(imageFile);

        Minecraft.getInstance().gui.setScreen(new Screen(Component.literal("Enlarged Image Screen")) {
            @Override
            public void init() {
                HeaderAndFooterLayout root = new HeaderAndFooterLayout(this, 20, 40);

                StringWidget title = new StringWidget(Component.literal(imageFile.getName()),Minecraft.getInstance().font);
                root.addToHeader(title);

                ImageWidget imageWidget = ImageUtils.getImageWidget(imageFile, (int) (this.height * 0.75));
                imageWidget.setTooltip(null);
                root.addToContents(imageWidget);

                Button doneButton = Button.builder(Component.literal("Done"), _ -> this.onClose())
                        .size(75, 20).build();
                root.addToFooter(doneButton);

                root.arrangeElements();
                FrameLayout.alignInRectangle(root, 0, 0, this.width, this.height, 0.5F, 0.1F);
                root.visitWidgets(this::addRenderableWidget);
            }

            @Override
            public void onClose() {
                Minecraft.getInstance().gui.setScreen(QuestionScreen.this);
            }
        });
    }
}
