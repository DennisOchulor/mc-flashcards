package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.Question;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;

public class EditScreen extends Screen {

    protected EditScreen() {
        super(Text.literal("Edit screen"));
        parent = MinecraftClient.getInstance().currentScreen;
    }

    private final Screen parent;
    final HashMap<String,List<Question>> map = FileManager.getQuestions();
    final CategoryListWidget categoryList = new CategoryListWidget(map);
    final QuestionListWidget questionList = new QuestionListWidget(map.get(categoryList.getSelectedOrNull().name));;
    private ButtonWidget categoryRenameButton;
    private ButtonWidget categoryAddButton;
    private ButtonWidget categoryDeleteButton;
    private ButtonWidget questionEditButton;
    private ButtonWidget questionDeleteButton;
    private ButtonWidget questionAddButton;
    private ButtonWidget applyButton;

    @Override
    protected void init() {
        categoryList.setPosition(width/2 - 230, 20);
        categoryList.setDimensions(75,100);

        questionList.setPosition(width/2 - 140, 20);
        questionList.setDimensions(275,210);

        categoryRenameButton = ButtonWidget.builder(Text.literal("Rename"),button -> {
            if(categoryList.getSelectedOrNull() == null) return;
            MinecraftClient.getInstance().setScreen(new CategoryRenameScreen(categoryList.getSelectedOrNull().name));
        }).build();
        categoryRenameButton.setDimensionsAndPosition(60,20,width/2 - 223,135);

        categoryAddButton = ButtonWidget.builder(Text.literal("Add"),button -> MinecraftClient.getInstance().setScreen(new CategoryAddScreen())).build();
        categoryAddButton.setDimensionsAndPosition(60,20,width/2 - 223,165);

        categoryDeleteButton = ButtonWidget.builder(Text.literal("Delete"),button -> {
            if(categoryList.getSelectedOrNull() == null) return;
            map.remove(categoryList.getSelectedOrNull().name);
            categoryList.children().remove(categoryList.getSelectedOrNull());
            categoryList.setSelected(null);
            questionList.children().clear();
        }).build();
        categoryDeleteButton.setDimensionsAndPosition(60,20,width/2 - 223,195);


        questionEditButton = ButtonWidget.builder(Text.literal("Edit"),button -> {
            if(questionList.getSelectedOrNull() == null) return;
            MinecraftClient.getInstance().setScreen(new QuestionEditScreen(questionList.getSelectedOrNull(),categoryList.getSelectedOrNull().name));
        }).build();
        questionEditButton.setDimensionsAndPosition(60,20,width/2 + 160,90);

        questionAddButton = ButtonWidget.builder(Text.literal("Add"),button -> MinecraftClient.getInstance().setScreen(new QuestionAddScreen(categoryList.getSelectedOrNull().name))).build();
        questionAddButton.setDimensionsAndPosition(60,20,width/2 + 160,120);

        questionDeleteButton = ButtonWidget.builder(Text.literal("Delete"),button -> {
            if(questionList.getSelectedOrNull() == null) return;
            map.get(categoryList.getSelectedOrNull().name).remove(questionList.getSelectedOrNull().index);
            questionList.children().remove(questionList.getSelectedOrNull().index);
            questionList.setSelected(null);
        }).build();
        questionDeleteButton.setDimensionsAndPosition(60,20,width/2 + 160,150);

        applyButton = ButtonWidget.builder(Text.literal("Apply Changes"),button -> {
            FileManager.updateQuestions(map);
            QuestionScheduler.reload();
            this.close();
        }).build();
        applyButton.setDimensionsAndPosition(120,20,width/2 - 60,235);

        addDrawableChild(categoryList);
        addDrawableChild(questionList);
        addDrawableChild(categoryRenameButton);
        addDrawableChild(categoryAddButton);
        addDrawableChild(categoryDeleteButton);
        addDrawableChild(questionEditButton);
        addDrawableChild(questionAddButton);
        addDrawableChild(questionDeleteButton);
        addDrawableChild(applyButton);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

}

