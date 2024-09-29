package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ImageUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class QuestionScreen extends Screen {
    private final TextWidget titleText = new TextWidget(Text.literal("Answer this question"), MinecraftClient.getInstance().textRenderer);
    private final MultilineTextWidget questionText;
    private final IconWidget imageWidget;
    private final EnlargeImageOnClickWidget enlargeImageOnClickWidget;
    private final ImageUtils.ImagePackage imgPkg;

    public QuestionScreen(Question question) {
        super(Text.literal("Question Prompt"));
        imgPkg = ImageUtils.getImageId(FileManager.getImage(question.imageName()));
        questionText = new MultilineTextWidget(Text.literal(question.question()), MinecraftClient.getInstance().textRenderer);

        if(question.imageName() != null) {
            if(imgPkg == null) {
                imageWidget = IconWidget.create(140,140, Identifier.ofVanilla("textures/missing.png"),140,140);
                imageWidget.setTooltip(Tooltip.of(Text.literal(question.imageName() + " seems to be missing...")));
                enlargeImageOnClickWidget = new EnlargeImageOnClickWidget(this,question.imageName() + " (missing)",new ImageUtils.ImagePackage(Identifier.ofVanilla("textures/missing.png"),1,1,1,1));
            }
            else {
                int width = (int)(140 * imgPkg.widthScaler());
                int height = (int)(140 * imgPkg.heightScaler());
                imageWidget = IconWidget.create(width,height,imgPkg.id(),width,height);
                imageWidget.setTooltip(Tooltip.of(Text.literal("Click to enlarge")));
                enlargeImageOnClickWidget = new EnlargeImageOnClickWidget(this,question.imageName(),imgPkg);
            }
        }
        else {
            imageWidget = null;
            enlargeImageOnClickWidget = null;
        }
    }

    @Override
    public void init() {
        titleText.alignCenter().setDimensionsAndPosition(width,10,0,15);

        questionText.setWidth(250);
        questionText.setMaxWidth(250);
        questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2, 45);
        questionText.setCentered(true);

        addDrawable(titleText);
        addDrawable(questionText);
        if(imageWidget != null) {
            addDrawable(imageWidget);
            addSelectableChild(enlargeImageOnClickWidget);
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
    public void close() {
        if(imgPkg != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(imgPkg.id());
        super.close();
    }

    static class EnlargeImageOnClickWidget extends ClickableWidget {
        // this is needed because the onClick() behaviour of an IconWidget cannot be modified at all...
        private final Screen parent;
        private final String imageName;
        private final ImageUtils.ImagePackage imgPkg;

        EnlargeImageOnClickWidget(Screen parent, String imageName, ImageUtils.ImagePackage imgPkg) {
            super(0,0,(int)(140 * imgPkg.widthScaler()),(int)(140 * imgPkg.heightScaler()),null);
            this.parent = parent;
            this.imageName = imageName;
            this.imgPkg = imgPkg;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {}
        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

        @Override
        public void onClick(double mouseX, double mouseY) {
            MinecraftClient.getInstance().setScreen(new Screen(Text.literal("Enlarged Image Screen")) {
                @Override
                public void init() {
                    TextWidget title = new TextWidget(Text.literal(imageName),MinecraftClient.getInstance().textRenderer);
                    title.setDimensionsAndPosition(width,15,0,0);
                    title.alignCenter();

                    int width = (int)(215 * imgPkg.widthScaler());
                    int height = (int)(215 * imgPkg.heightScaler());
                    IconWidget image = IconWidget.create(width,height,imgPkg.id(),width,height);
                    image.setPosition(this.width/2 - width/2,15);

                    ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
                        MinecraftClient.getInstance().setScreen(parent);
                    }).dimensions(this.width/2 - 37,240,75,20).build();

                    addDrawable(title);
                    addDrawable(image);
                    addDrawableChild(doneButton);
                }

                @Override
                public void close() {
                    MinecraftClient.getInstance().setScreen(parent);
                }
            });
        }
    }
}
