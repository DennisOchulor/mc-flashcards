package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.HashMap;
import java.util.List;

class CategoryListWidget extends AlwaysSelectedEntryListWidget<CategoryListWidget.Entry> {
    CategoryListWidget(HashMap<String, List<Question>> map) {
        super(MinecraftClient.getInstance(), 75, 100, 20, 11);
        map.keySet().forEach(category -> {
            this.children().add(new Entry(category));
            this.setSelected(this.children().getFirst());
        });
    }

    static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        String name;

        Entry(String name) {
            this.name = name;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", "a category entry");
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawText(MinecraftClient.getInstance().textRenderer, name, x+90, y, Colors.WHITE, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            EditScreen screen = (EditScreen) MinecraftClient.getInstance().currentScreen;
            screen.questionList.changeList(screen.map.get(name));
            return true;
        }
    }

}
