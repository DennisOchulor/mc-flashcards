package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public class ScalableMultilineTextWidget extends MultiLineTextWidget {

    private final int maxHeigth;

    public ScalableMultilineTextWidget(Component message, Font textRenderer, int maxHeight) {
        super(message, textRenderer);
        this.maxHeigth = maxHeight;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (this.getHeight() > maxHeigth) {
            // https://stackoverflow.com/questions/56824983/scaling-is-moving-the-object
            float scale = (float) maxHeigth / getHeight();
            context.pose().pushMatrix();
            Matrix3x2fStack positionMatrix = context.pose(); // get position matrix?
            Matrix3x2f transRefToOrigin = new Matrix3x2f().translate(-getX(),-getY());
            Matrix3x2f scalingMatrix = new Matrix3x2f().scale(scale);
            float widthDiff = (getWidth() - (getWidth() * scale)) / 2.0f; // to center it properly again
            Matrix3x2f transOriginToPos = new Matrix3x2f().translate(getX() + widthDiff, getY());
            positionMatrix.set(transOriginToPos.mul(scalingMatrix).mul(transRefToOrigin));
        }
        super.renderWidget(context,mouseX,mouseY,delta);
        if (this.getHeight() > maxHeigth) context.pose().popMatrix();
    }

}
