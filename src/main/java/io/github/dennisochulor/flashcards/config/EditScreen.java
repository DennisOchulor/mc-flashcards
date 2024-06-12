package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.Question;
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
    }

    QuestionListWidget questionList;
    private CategoryListWidget categoryList;
    final HashMap<String,List<Question>> map = FileManager.getQuestions();
    private ButtonWidget categoryRenameButton;
    private ButtonWidget categoryAddButton;
    private ButtonWidget categoryDeleteButton;
    private ButtonWidget questionEditButton;
    private ButtonWidget questionDeleteButton;
    private ButtonWidget questionAddButton;
    private ButtonWidget applyButton;

    @Override
    protected void init() {
        categoryList = new CategoryListWidget(map);
        categoryList.setPosition(width/2 - 230, 20);
        categoryList.setDimensions(75,100);

        questionList = new QuestionListWidget(map.get(categoryList.getSelectedOrNull().name));
        questionList.setPosition(width/2 - 140, 20);
        questionList.setDimensions(275,210);

        categoryRenameButton = ButtonWidget.builder(Text.literal("Rename"),button -> {

        }).build();
        categoryRenameButton.setDimensionsAndPosition(60,20,width/2 - 223,135);

        categoryAddButton = ButtonWidget.builder(Text.literal("Add"),button -> {

        }).build();
        categoryAddButton.setDimensionsAndPosition(60,20,width/2 - 223,165);

        categoryDeleteButton = ButtonWidget.builder(Text.literal("Delete"),button -> {

        }).build();
        categoryDeleteButton.setDimensionsAndPosition(60,20,width/2 - 223,195);


        questionEditButton = ButtonWidget.builder(Text.literal("Edit"),button -> {

        }).build();
        questionEditButton.setDimensionsAndPosition(60,20,width/2 + 160,90);

        questionAddButton = ButtonWidget.builder(Text.literal("Add"),button -> {

        }).build();
        questionAddButton.setDimensionsAndPosition(60,20,width/2 + 160,120);

        questionDeleteButton = ButtonWidget.builder(Text.literal("Delete"),button -> {

        }).build();
        questionDeleteButton.setDimensionsAndPosition(60,20,width/2 + 160,150);

        applyButton = ButtonWidget.builder(Text.literal("Apply Changes"),button -> {

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
        return true;
    }

}
