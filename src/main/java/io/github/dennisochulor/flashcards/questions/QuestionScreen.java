package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class QuestionScreen extends Screen {
    private final StringWidget titleText = new StringWidget(Component.literal("Answer this question"), Minecraft.getInstance().font);
    private final MultiLineTextWidget questionText;
    @Nullable
    private final ImageWidget imageWidget;
    @Nullable
    private final EnlargeImageOnClickWidget enlargeImageOnClickWidget;
    private final ImageUtils.@Nullable ImagePackage imgPkg;

    public QuestionScreen(Question question) {
        super(Component.literal("Question Prompt"));
        questionText = new ScalableMultilineTextWidget(Component.literal(question.question()), Minecraft.getInstance().font, 100);

        if(question.imageName() != null) {
            imgPkg = ImageUtils.getImagePackage(FileManager.getImageFile(question.imageName()));
            if(imgPkg == null) {
                imageWidget = ImageWidget.texture(140,140, Identifier.withDefaultNamespace("textures/missing.png"),140,140);
                imageWidget.setTooltip(Tooltip.create(Component.literal(question.imageName() + " seems to be missing...")));
                enlargeImageOnClickWidget = new EnlargeImageOnClickWidget(this,question.imageName() + " (missing)",new ImageUtils.ImagePackage(Identifier.withDefaultNamespace("textures/missing.png"),1,1,1,1));
            }
            else {
                int width = (int)(140 * imgPkg.widthScaler());
                int height = (int)(140 * imgPkg.heightScaler());
                imageWidget = ImageWidget.texture(width,height,imgPkg.id(),width,height);
                imageWidget.setTooltip(Tooltip.create(Component.literal("Click to enlarge")));
                enlargeImageOnClickWidget = new EnlargeImageOnClickWidget(this,question.imageName(),imgPkg);
            }
        }
        else {
            imageWidget = null;
            imgPkg = null;
            enlargeImageOnClickWidget = null;
        }
    }

    @Override
    public void init() {
        titleText.setPosition(width/2 - titleText.getWidth()/2,15);

        questionText.setWidth(250);
        questionText.setMaxWidth(250);
        questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2, 45);
        questionText.setCentered(true);

        addRenderableOnly(titleText);
        addRenderableOnly(questionText);
        if(imageWidget != null) {
            addRenderableOnly(imageWidget);
            addWidget(Objects.requireNonNull(enlargeImageOnClickWidget));
            questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2 + 100, 45);
            imageWidget.setPosition(width/4 - 75,25);
            enlargeImageOnClickWidget.setPosition(width/4 - 75,25);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        if(imgPkg != null) Minecraft.getInstance().getTextureManager().release(imgPkg.id());
        super.onClose();
    }

    static class EnlargeImageOnClickWidget extends AbstractWidget {
        // this is needed because the onClick() behaviour of an IconWidget cannot be modified at all...
        private final Screen parent;
        private final String imageName;
        private final ImageUtils.ImagePackage imgPkg;

        EnlargeImageOnClickWidget(Screen parent, String imageName, ImageUtils.ImagePackage imgPkg) {
            super(0,0,(int)(140 * imgPkg.widthScaler()),(int)(140 * imgPkg.heightScaler()),Component.empty());
            this.parent = parent;
            this.imageName = imageName;
            this.imgPkg = imgPkg;
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {}
        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}

        @Override
        public void onClick(MouseButtonEvent click, boolean bl) {
            Minecraft.getInstance().setScreen(new Screen(Component.literal("Enlarged Image Screen")) {
                @Override
                public void init() {
                    StringWidget title = new StringWidget(Component.literal(imageName),Minecraft.getInstance().font);
                    title.setPosition(width/2 - title.getWidth()/2,5);

                    int width = (int)(215 * imgPkg.widthScaler());
                    int height = (int)(215 * imgPkg.heightScaler());
                    ImageWidget image = ImageWidget.texture(width,height,imgPkg.id(),width,height);
                    image.setPosition(this.width/2 - width/2,15);

                    Button doneButton = Button.builder(Component.literal("Done"), _ -> {
                        Minecraft.getInstance().setScreen(parent);
                    }).bounds(this.width/2 - 37,240,75,20).build();

                    addRenderableOnly(title);
                    addRenderableOnly(image);
                    addRenderableWidget(doneButton);
                }

                @Override
                public void onClose() {
                    Minecraft.getInstance().setScreen(parent);
                }
            });
        }
    }
}
