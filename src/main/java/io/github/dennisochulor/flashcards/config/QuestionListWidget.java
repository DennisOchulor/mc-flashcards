package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

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

    @Override
    public int getRowWidth() {
        return width - 20;
    }


    static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        Question question;

        Entry(Question question) {
            this.question = question;
        }

        @Override
        public Text getNarration() {
            return Text.literal(question.question());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            TextWidget text = new TextWidget(Text.of(question.question()), MinecraftClient.getInstance().textRenderer);
            text.setPosition(getContentX(),getContentY());
            text.setMaxWidth(getContentWidth());
            text.render(context, mouseX, mouseY, deltaTicks);
        }


    }

}
