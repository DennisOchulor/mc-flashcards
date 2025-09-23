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
        this.clearEntries();
        this.replaceEntries(questions.stream().map(Entry::new).toList());
    }

    public void add(QuestionListWidget.Entry entry) {
        this.addEntry(entry);
    }

    public void remove(QuestionListWidget.Entry entry) {
        this.removeEntry(entry);
    }

    static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        Question question;

        Entry(Question question) {
            this.question = question;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", "a question entry");
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            if(question.question().length() > 38) context.drawText(MinecraftClient.getInstance().textRenderer, question.question().substring(0,38) + "...", getContentX(), getContentY(), Colors.WHITE, false);
            else context.drawText(MinecraftClient.getInstance().textRenderer, question.question(), getContentX(), getContentY(), Colors.WHITE, false);
        }
    }

}
