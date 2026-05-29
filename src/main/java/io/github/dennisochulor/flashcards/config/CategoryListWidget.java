package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

class CategoryListWidget extends ObjectSelectionList<CategoryListWidget.Entry> {
    CategoryListWidget(Map<String, List<Question>> map, Map<String,Boolean> categoryToggle) {
        super(Minecraft.getInstance(), 75, 100, 20, 11);
        map.keySet().forEach(category -> {
            this.add(new CategoryListWidget.Entry(category,categoryToggle.getOrDefault(category,true)));
        });
        this.setSelected(this.children().getFirst());
    }

    public void add(CategoryListWidget.Entry entry) {
        this.addEntry(entry);
    }

    public void remove(CategoryListWidget.Entry entry) {
        this.removeEntry(entry);
    }

    static class Entry extends ObjectSelectionList.Entry<CategoryListWidget.Entry> {
        String name;
        boolean enabled;

        Entry(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", "Category entry " + name);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            context.text(Minecraft.getInstance().font, name, getContentX()+75, getContentY(), CommonColors.WHITE, false);
            if (enabled) context.text(Minecraft.getInstance().font, "✔", getContentX()+140, getContentY(), CommonColors.GREEN, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            EditScreen screen = (EditScreen) Objects.requireNonNull(Minecraft.getInstance().gui.screen());
            screen.questionList.changeList(screen.categoriesMap.get(name));
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_1 && click.x()>=getContentX()+140) {
                enabled = !enabled;
            }
            return true;
        }
    }

}
