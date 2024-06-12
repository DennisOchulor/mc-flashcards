package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.List;

class QuestionListWidget extends AlwaysSelectedEntryListWidget<QuestionListWidget.Entry> {

    QuestionListWidget(List<Question> questions) {
        super(MinecraftClient.getInstance(), 100, 200, 20, 11);
        changeList(questions);
    }

    void changeList(List<Question> questions) {
        this.children().clear();
        for(int i=0; i<questions.size(); i++) {
            this.children().add(new Entry(questions.get(i),i));
        }
    }

    static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        final Question question;
        int index;

        Entry(Question question, int index) {
            this.question = question;
            this.index = index;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", "a question entry");
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawText(MinecraftClient.getInstance().textRenderer, question.question(), x, y, Colors.WHITE, false);
        }
    }

}
