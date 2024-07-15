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
    private final EditBoxWidget answerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,200,50,Text.literal("Write your answer here..."),Text.empty());
    private final ButtonWidget submitButton;
    private final MultilineTextWidget questionText;
    private final IconWidget imageWidget;
    private final EnlargeImageOnClickWidget enlargeImageOnClickWidget;

    public QuestionScreen(Question question) {
        super(Text.literal("Question Prompt"));
        ImageUtils.ImagePackage imgPkg = ImageUtils.getImageId(FileManager.getImage(question.imageName()));

        questionText = new MultilineTextWidget(Text.literal(question.question()), MinecraftClient.getInstance().textRenderer);
        submitButton = ButtonWidget.builder(Text.literal("Submit"), button -> {
            if(imgPkg != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(imgPkg.id());
            MinecraftClient.getInstance().setScreen(new ResultScreen(question, answerEditBox.getText()));
        }).build();

        if(question.imageName() != null) {
            if(imgPkg == null) {
                imageWidget = IconWidget.create(140,140,Identifier.ofVanilla("textures/missing.png"),140,140);
                imageWidget.setTooltip(Tooltip.of(Text.literal(question.imageName() + " seems to be missing...")));
                enlargeImageOnClickWidget = null;
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

    private static class EnlargeImageOnClickWidget extends ClickableWidget {
        // this is needed because the onClick() behaviour of an IconWidget cannot be modified at all...
        private final Screen parent;
        private final String imageName;
        private final ImageUtils.ImagePackage imgPkg;

        EnlargeImageOnClickWidget(Screen parent, String imageName, ImageUtils.ImagePackage imgPkg) {
            super(0,0,imgPkg.width(),imgPkg.height(),null);
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

                    ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"),button -> {
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
