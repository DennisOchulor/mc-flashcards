package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
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
            this.children().add(new Entry(category,categoryToggle.get(category)));
        });
        this.setSelected(this.children().getFirst());
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
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawText(MinecraftClient.getInstance().textRenderer, name, x+75, y, Colors.WHITE, false);
            if(enabled) context.drawText(MinecraftClient.getInstance().textRenderer, "✔", x+135, y, Colors.GREEN, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            EditScreen screen = (EditScreen) MinecraftClient.getInstance().currentScreen;
            screen.questionList.changeList(screen.map.get(name));
            if(button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseX>=73) { // x is -61, then plus 135, minus a little
                enabled = !enabled;
            }
            return true;
        }
    }

}
