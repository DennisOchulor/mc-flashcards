package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
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
            context.getMatrices().pushMatrix();
            Matrix3x2fStack positionMatrix = context.getMatrices(); // get position matrix?
            Matrix3x2f transRefToOrigin = new Matrix3x2f().translate(-getX(),-getY());
            Matrix3x2f scalingMatrix = new Matrix3x2f().scale(scale);
            float widthDiff = (getWidth() - (getWidth() * scale)) / 2.0f; // to center it properly again
            Matrix3x2f transOriginToPos = new Matrix3x2f().translate(getX() + widthDiff, getY());
            positionMatrix.set(transOriginToPos.mul(scalingMatrix).mul(transRefToOrigin));
        }
        super.renderWidget(context,mouseX,mouseY,delta);
        if(this.getHeight() > maxHeigth) context.getMatrices().popMatrix();
    }

}
