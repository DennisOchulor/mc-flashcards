package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.resources.Identifier;

public class QuestionScreen extends Screen {
    private static final int QUESTION_TEXT_WIDTH = 250;

    private final StringWidget titleText = new StringWidget(Component.literal("Answer this question"), Minecraft.getInstance().font);
    private final MultiLineTextWidget questionText;
    private final Question question;

    public QuestionScreen(Question question) {
        super(Component.literal("Question Prompt"));
        this.question = question;
        questionText = new ScalableMultilineTextWidget(Component.literal(question.question()), Minecraft.getInstance().font, 100);
        questionText.setCentered(true).setMaxWidth(QUESTION_TEXT_WIDTH);
    }

    @Override
    public void init() {
        int partHeight = (int) (0.3 * this.height);

        LinearLayout questionLayout = LinearLayout.horizontal().spacing(20);
        questionLayout.defaultCellSetting().alignHorizontallyCenter();
        if (question.imageName() != null) {
            questionLayout.addChild(new EnlargeableImageWidget(this, question.imageName(),
                    ImageUtils.getImageWidget(question.imageName(), partHeight)));

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

    static class EnlargeableImageWidget extends ImageWidget {
        // this is needed because the onClick() behaviour of an ImageWidget cannot be modified at all...
        private final Screen parent;
        private final String imageName;
        private final ImageWidget wrapped;

        EnlargeableImageWidget(Screen parent, String imageName, ImageWidget wrapped) {
            super(wrapped.getX(), wrapped.getY(), wrapped.getWidth(), wrapped.getHeight());

            this.parent = parent;
            this.imageName = imageName;
            this.wrapped = wrapped;

            setTooltip(Tooltip.create(Component.literal("Click to enlarge")));
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            wrapped.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
        }

        @Override
        public void updateResource(Identifier identifier) {
            wrapped.updateResource(identifier);
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            wrapped.setX(x);
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            wrapped.setY(y);
        }

        @Override
        public void onClick(MouseButtonEvent click, boolean bl) {
            Minecraft.getInstance().gui.setScreen(new Screen(Component.literal("Enlarged Image Screen")) {
                @Override
                public void init() {
                    HeaderAndFooterLayout root = new HeaderAndFooterLayout(this, 20, 40);

                    StringWidget title = new StringWidget(Component.literal(imageName),Minecraft.getInstance().font);
                    root.addToHeader(title);

                    ImageWidget imageWidget = ImageUtils.getImageWidget(imageName, (int) (this.height * 0.75));
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
                    Minecraft.getInstance().gui.setScreen(parent);
                }
            });
        }
    }
}
