package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public class ScalableMultilineTextWidget extends MultilineTextWidget {

    private final int maxHeigth;

    public ScalableMultilineTextWidget(Text message, TextRenderer textRenderer, int maxHeight) {
        super(message, textRenderer);
        this.maxHeigth = maxHeight;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if(this.getHeight() > maxHeigth) {
            // https://stackoverflow.com/questions/56824983/scaling-is-moving-the-object
            float scale = (float) maxHeigth / getHeight();
            context.getMatrices().push();
            Matrix4f positionMatrix =  context.getMatrices().peek().getPositionMatrix();
            Matrix4f transRefToOrigin = new Matrix4f().translate(-getX(),-getY(),-0);
            Matrix4f scalingMatrix = new Matrix4f().scale(scale);
            float widthDiff = (getWidth() - (getWidth() * scale)) / 2.0f; // to center it properly again
            Matrix4f transOriginToPos = new Matrix4f().translate(getX() + widthDiff, getY(), 0);
            positionMatrix.set(transOriginToPos.mul(scalingMatrix).mul(transRefToOrigin));
        }
        super.renderWidget(context,mouseX,mouseY,delta);
        if(this.getHeight() > maxHeigth) context.getMatrices().pop();
    }

}
