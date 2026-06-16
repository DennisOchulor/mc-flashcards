package io.github.dennisochulor.flashcards.questions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public class ScalableMultilineTextWidget extends MultiLineTextWidget {

    private int maxHeigth;

    public ScalableMultilineTextWidget(Component message, Font textRenderer, int maxHeight) {
        super(message, textRenderer);
        this.maxHeigth = maxHeight;
    }

    public void setMaxHeigth(int maxHeigth) {
        this.maxHeigth = maxHeigth;
    }

    @Override
    public int getHeight() {
        return shouldScale() ? Math.round(super.getHeight() * getScale()) : super.getHeight();
    }

    @Override
    public int getWidth() {
        return shouldScale() ? Math.round(super.getWidth() * getScale()) : super.getWidth();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (shouldScale()) {
            // https://stackoverflow.com/questions/56824983/scaling-is-moving-the-object
            float scale = getScale();
            context.pose().pushMatrix();
            Matrix3x2fStack positionMatrix = context.pose(); // get position matrix?
            Matrix3x2f transRefToOrigin = new Matrix3x2f().translate(-getX(),-getY());
            Matrix3x2f scalingMatrix = new Matrix3x2f().scale(scale);
            float widthDiff = (super.getWidth() - (super.getWidth() * scale)) / 2.0f; // to center it properly again
            Matrix3x2f transOriginToPos = new Matrix3x2f().translate(getX() + widthDiff, getY());
            positionMatrix.set(transOriginToPos.mul(scalingMatrix).mul(transRefToOrigin));
        }
        super.extractWidgetRenderState(context,mouseX,mouseY,delta);
        if (shouldScale()) context.pose().popMatrix();
    }

    private boolean shouldScale() {
        return super.getHeight() > maxHeigth;
    }

    private float getScale() {
        return (float) maxHeigth / super.getHeight();
    }


}
