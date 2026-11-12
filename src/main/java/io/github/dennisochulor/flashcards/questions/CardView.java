package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ImageUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrollable panel that shows a flashcard at the normal font size: blocks of text, images and dividers stacked vertically.
 * Text that wraps onto more than a few lines is left-aligned (easier to read); short text is centered like in Anki.
 */
public class CardView extends AbstractScrollArea {
    private static final int PADDING = 8;
    private static final int BLOCK_GAP = 8;
    private static final int CENTER_MAX_LINES = 3;
    private static final int BACKGROUND = 0xC0101010;
    private static final int BORDER = 0x40FFFFFF;
    private static final int DIVIDER = 0x60FFFFFF;

    /** Something to show on the card. */
    public sealed interface Block permits Text, Image, Divider {}

    /** @param left always left-align (e.g. lists); otherwise short text is centered */
    public record Text(Component text, int color, boolean left) implements Block {
        public Text(Component text, int color) {
            this(text, color, false);
        }
    }

    /** @param imageName relative to the mod's media folder */
    public record Image(String imageName, int maxSize) implements Block {}

    public record Divider() implements Block {}

    private record Laid(Block block, int y, int height, List<FormattedCharSequence> lines, boolean centered, ImageWidget image) {}

    private final Font font;
    private final List<Block> blocks = new ArrayList<>();
    private final List<Laid> laid = new ArrayList<>();
    private int laidWidth = -1;
    private int laidContentHeight = 0;

    public CardView(Font font) {
        super(0, 0, 100, 100, Component.empty(), AbstractScrollArea.defaultSettings(font.lineHeight * 2));
        this.font = font;
    }

    public void setBlocks(List<Block> newBlocks) {
        blocks.clear();
        blocks.addAll(newBlocks);
        laidWidth = -1;
        StringBuilder narration = new StringBuilder();
        for (Block block : blocks) if (block instanceof Text t) narration.append(t.text().getString()).append('\n');
        setMessage(Component.literal(narration.toString()));
    }

    /** Height the content needs at the current width, so the screen can shrink the panel for short cards. */
    public int preferredHeight() {
        return contentHeight();
    }

    private int innerWidth() {
        return Math.max(20, width - PADDING * 2 - scrollbarWidth());
    }

    private void ensureLayout() {
        int innerWidth = innerWidth();
        if (laidWidth == innerWidth) return;
        laidWidth = innerWidth;
        laid.clear();

        int y = 0;
        for (Block block : blocks) {
            if (!laid.isEmpty()) y += BLOCK_GAP;
            switch (block) {
                case Text text -> {
                    List<FormattedCharSequence> lines = font.split(text.text(), innerWidth);
                    int height = lines.size() * font.lineHeight;
                    laid.add(new Laid(block, y, height, lines, !text.left() && lines.size() <= CENTER_MAX_LINES, null));
                    y += height;
                }
                case Image image -> {
                    ImageWidget widget = ImageUtils.getImageWidget(FileManager.getImageFile(image.imageName()), image.maxSize());
                    if (widget.getWidth() > innerWidth) { // too wide: shrink, keeping the aspect ratio
                        int size = Math.max(16, image.maxSize() * innerWidth / widget.getWidth());
                        widget = ImageUtils.getImageWidget(FileManager.getImageFile(image.imageName()), size);
                    }
                    widget.setTooltip(null);
                    laid.add(new Laid(block, y, widget.getHeight(), List.of(), true, widget));
                    y += widget.getHeight();
                }
                case Divider _ -> {
                    laid.add(new Laid(block, y, 1, List.of(), true, null));
                    y += 1;
                }
            }
        }
        laidContentHeight = y;
    }

    @Override
    protected int contentHeight() {
        ensureLayout();
        return laidContentHeight + PADDING * 2;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        ensureLayout();
        graphics.fill(getX(), getY(), getRight(), getBottom(), BACKGROUND);
        graphics.outline(getX(), getY(), width, height, BORDER);

        graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        int left = getX() + PADDING;
        int innerWidth = innerWidth();
        int top = getY() + PADDING - (int) scrollAmount();
        for (Laid item : laid) {
            int y = top + item.y();
            if (y > getBottom() || y + item.height() < getY()) continue; // off screen
            switch (item.block()) {
                case Text text -> {
                    int lineY = y;
                    for (FormattedCharSequence line : item.lines()) {
                        int lineX = item.centered() ? left + (innerWidth - font.width(line)) / 2 : left;
                        graphics.text(font, line, lineX, lineY, text.color(), true);
                        lineY += font.lineHeight;
                    }
                }
                case Image _ -> {
                    ImageWidget image = item.image();
                    image.setPosition(left + (innerWidth - image.getWidth()) / 2, y);
                    image.extractRenderState(graphics, mouseX, mouseY, delta);
                }
                case Divider _ -> graphics.fill(left, y, left + innerWidth, y + 1, DIVIDER);
            }
        }
        graphics.disableScissor();
        extractScrollbar(graphics, mouseX, mouseY);
    }

    public void scrollBy(double amount) {
        setScrollAmount(scrollAmount() + amount);
    }

    /** @return the block's top relative to the visible area when not scrolled, or -1 for a bad index */
    public int blockTop(int index) {
        ensureLayout();
        return index >= 0 && index < laid.size() ? PADDING + laid.get(index).y() : -1;
    }

    /** Scrolls so the given block (by index) starts near the top, e.g. to jump to the answer. */
    public void scrollToBlock(int index) {
        int top = blockTop(index);
        if (top >= 0) setScrollAmount(top - PADDING);
    }

    public int pageHeight() {
        return Math.max(font.lineHeight, height - PADDING * 2 - font.lineHeight);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean scrollbar = updateScrolling(event); // starts dragging the scrollbar
        return super.mouseClicked(event, doubleClick) || scrollbar;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // clicking the card isn't a button press
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
