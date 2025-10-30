package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

class QuestionListWidget extends ObjectSelectionList<QuestionListWidget.Entry> {

    QuestionListWidget(List<Question> questions) {
        super(Minecraft.getInstance(), 100, 200, 20, 11);
        changeList(questions);
    }

    void changeList(List<Question> questions) {
        this.clearEntries();
        this.replaceEntries(questions.stream().map(io.github.dennisochulor.flashcards.config.QuestionListWidget.Entry::new).toList());
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


    static class Entry extends ObjectSelectionList.Entry<io.github.dennisochulor.flashcards.config.QuestionListWidget.Entry> {
        Question question;

        Entry(Question question) {
            this.question = question;
        }

        @Override
        public Component getNarration() {
            return Component.literal(question.question());
        }

        @Override
        public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            StringWidget text = new StringWidget(Component.nullToEmpty(question.question()), Minecraft.getInstance().font);
            text.setPosition(getContentX(),getContentY());
            text.setMaxWidth(getContentWidth());
            text.render(context, mouseX, mouseY, deltaTicks);
        }


    }

}
