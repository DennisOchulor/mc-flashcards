package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

class CategoryListWidget extends ObjectSelectionList<CategoryListWidget.Entry> {
    CategoryListWidget(HashMap<String, List<Question>> map, HashMap<String,Boolean> categoryToggle) {
        super(Minecraft.getInstance(), 75, 100, 20, 11);
        map.keySet().forEach(category -> {
            this.add(new io.github.dennisochulor.flashcards.config.CategoryListWidget.Entry(category,categoryToggle.getOrDefault(category,true)));
        });
        this.setSelected(this.children().getFirst());
    }

    public void add(io.github.dennisochulor.flashcards.config.CategoryListWidget.Entry entry) {
        this.addEntry(entry);
    }

    public void remove(io.github.dennisochulor.flashcards.config.CategoryListWidget.Entry entry) {
        this.removeEntry(entry);
    }

    static class Entry extends ObjectSelectionList.Entry<io.github.dennisochulor.flashcards.config.CategoryListWidget.Entry> {
        String name;
        boolean enabled;

        Entry(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", "a category entry");
        }

        @Override
        public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            context.drawString(Minecraft.getInstance().font, name, getContentX()+75, getContentY(), CommonColors.WHITE, false);
            if(enabled) context.drawString(Minecraft.getInstance().font, "✔", getContentX()+140, getContentY(), CommonColors.GREEN, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            EditScreen screen = (EditScreen) Minecraft.getInstance().screen;
            screen.questionList.changeList(screen.categoriesMap.get(name));
            if(click.button() == GLFW.GLFW_MOUSE_BUTTON_1 && click.x()>=getContentX()+140) {
                enabled = !enabled;
            }
            return true;
        }
    }

}
