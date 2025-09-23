package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;

class CategoryListWidget extends AlwaysSelectedEntryListWidget<CategoryListWidget.Entry> {
    CategoryListWidget(HashMap<String, List<Question>> map, HashMap<String,Boolean> categoryToggle) {
        super(MinecraftClient.getInstance(), 75, 100, 20, 11);
        map.keySet().forEach(category -> {
            this.add(new Entry(category,categoryToggle.getOrDefault(category,true)));
        });
        this.setSelected(this.children().getFirst());
    }

    public void add(Entry entry) {
        this.addEntry(entry);
    }

    public void remove(Entry entry) {
        this.removeEntry(entry);
    }

    static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        String name;
        boolean enabled;

        Entry(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", "a category entry");
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            context.drawText(MinecraftClient.getInstance().textRenderer, name, getContentX()+75, getContentY(), Colors.WHITE, false);
            if(enabled) context.drawText(MinecraftClient.getInstance().textRenderer, "✔", getContentX()+140, getContentY(), Colors.GREEN, false);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            EditScreen screen = (EditScreen) MinecraftClient.getInstance().currentScreen;
            screen.questionList.changeList(screen.categoriesMap.get(name));
            if(click.button() == GLFW.GLFW_MOUSE_BUTTON_1 && click.x()>=getContentX()+140) {
                enabled = !enabled;
            }
            return true;
        }
    }

}
