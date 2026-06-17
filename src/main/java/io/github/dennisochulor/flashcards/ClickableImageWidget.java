package io.github.dennisochulor.flashcards;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

// this is needed because the onClick() behaviour of an ImageWidget cannot be modified at all...
public class ClickableImageWidget extends ImageWidget {
    @FunctionalInterface
    public interface ImageWidgetClickListener {
        void onImageWidgetClick(MouseButtonEvent event, boolean doubleClick);
    }

    private final ImageWidget wrapped;
    private final ImageWidgetClickListener onClickAction;

    public ClickableImageWidget(ImageWidget wrapped, ImageWidgetClickListener onClickAction) {
        super(wrapped.getX(), wrapped.getY(), wrapped.getWidth(), wrapped.getHeight());
        setTooltip(wrapped.tooltip.get());

        this.wrapped = wrapped;
        this.onClickAction = onClickAction;
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
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        onClickAction.onImageWidgetClick(click, doubleClick);
    }
}
