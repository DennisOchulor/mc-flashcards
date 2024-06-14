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
        this.setSelected(null);
        questions.forEach(q -> this.children().add(new Entry(q)));
    }

    static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        Question question;
        int index;

        Entry(Question question) {
            this.question = question;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", "a question entry");
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.index = index;
            if(question.question().length() > 38) context.drawText(MinecraftClient.getInstance().textRenderer, question.question().substring(0,38) + "...", x, y, Colors.WHITE, false);
            else context.drawText(MinecraftClient.getInstance().textRenderer, question.question(), x, y, Colors.WHITE, false);
        }
    }

}
