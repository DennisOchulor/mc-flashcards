package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.Question;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;

public class EditScreen extends Screen {

    protected EditScreen() {
        super(Text.literal("Edit screen"));
        parent = MinecraftClient.getInstance().currentScreen;
    }

    private final Screen parent;
    final HashMap<String,List<Question>> categoriesMap = FileManager.getQuestions();
    private final ModConfig config = FileManager.getConfig();
    final CategoryListWidget categoryList = new CategoryListWidget(categoriesMap,config.categoryToggle());
    final QuestionListWidget questionList = new QuestionListWidget(categoriesMap.get(categoryList.getSelectedOrNull().name));
    private final TextWidget categoryTitle = new TextWidget(Text.literal("Categories"),MinecraftClient.getInstance().textRenderer);
    private final TextWidget questionTitle = new TextWidget(Text.literal("Questions"),MinecraftClient.getInstance().textRenderer);

    private final ButtonWidget categoryRenameButton = ButtonWidget.builder(Text.literal("Rename"),button -> {
        if(categoryList.getSelectedOrNull() == null) return;
        MinecraftClient.getInstance().setScreen(new CategoryRenameScreen(categoryList.getSelectedOrNull().name));
    }).build();
    private final ButtonWidget categoryAddButton = ButtonWidget.builder(Text.literal("Add"), button -> MinecraftClient.getInstance().setScreen(new CategoryAddScreen())).build();
    private final ButtonWidget categoryDeleteButton = ButtonWidget.builder(Text.literal("Delete"), button -> {
        if(categoryList.getSelectedOrNull() == null) return;
        categoriesMap.remove(categoryList.getSelectedOrNull().name);
        categoryList.remove(categoryList.getSelectedOrNull());
        categoryList.setSelected(null);
        questionList.changeList(List.of()); // clear
        questionList.setSelected(null);
    }).build();

    private final ButtonWidget questionEditButton = ButtonWidget.builder(Text.literal("Edit"), button -> {
        if(questionList.getSelectedOrNull() == null) return;
        MinecraftClient.getInstance().setScreen(new QuestionEditScreen(questionList.getSelectedOrNull(),categoryList.getSelectedOrNull().name));
    }).build();
    private final ButtonWidget questionDeleteButton = ButtonWidget.builder(Text.literal("Delete"), button -> {
        if(questionList.getSelectedOrNull() == null) return;
        categoriesMap.get(categoryList.getSelectedOrNull().name).remove(questionList.getSelectedOrNull().question);
        questionList.remove(questionList.getSelectedOrNull());
        questionList.setSelected(null);
    }).build();
    private final ButtonWidget questionAddButton = ButtonWidget.builder(Text.literal("Add"), button -> MinecraftClient.getInstance().setScreen(new QuestionAddScreen(categoryList.getSelectedOrNull().name))).build();

    private final ButtonWidget applyButton = ButtonWidget.builder(Text.literal("Apply Changes"), button -> {
        FileManager.updateQuestions(categoriesMap);

        HashMap<String,Boolean> categoryToggle = new HashMap<>();
        for(var category : categoryList.children()) {
            categoryToggle.put(category.name,category.enabled);
        }

        ModConfig newConfig = new ModConfig(config.interval(),config.intervalToggle(),config.validationToggle(),categoryToggle,config.correctAnswerCommands(),config.wrongAnswerCommands(),config.commandSelectionStrategy());
        FileManager.updateConfig(newConfig);
        QuestionScheduler.reload();
        QuestionScheduler.updateConfig(newConfig);
        this.close();
    }).build();

    @Override
    protected void init() {
        categoryList.position(85, 100, width/4 - 100, 20);
        questionList.position(width/2, height - 75, width/2 - width/4, 20);

        categoryTitle.setPosition(categoryList.getWidth()/2 + categoryList.getX() - categoryTitle.getWidth()/2, categoryList.getY() - 15);
        questionTitle.setPosition(questionList.getWidth()/2 + questionList.getX() - questionTitle.getWidth()/2, questionList.getY() - 15);

        int categoryButtonsX = categoryList.getX() + categoryList.getWidth()/2 - 30;
        categoryRenameButton.setDimensionsAndPosition(60,20,categoryButtonsX,categoryList.getY() + categoryList.getHeight() + 20);
        categoryAddButton.setDimensionsAndPosition(60,20,categoryButtonsX,categoryRenameButton.getY() + 30);
        categoryDeleteButton.setDimensionsAndPosition(60,20,categoryButtonsX,categoryAddButton.getY() + 30);

        int questionButtonsX = questionList.getX() + questionList.getWidth() + 20;
        int questionButtonsMiddleY = questionList.getY() + questionList.getHeight()/2 - 10;
        questionEditButton.setDimensionsAndPosition(60,20,questionButtonsX,questionButtonsMiddleY - 30);
        questionAddButton.setDimensionsAndPosition(60,20,questionButtonsX,questionButtonsMiddleY);
        questionDeleteButton.setDimensionsAndPosition(60,20,questionButtonsX,questionButtonsMiddleY + 30);

        applyButton.setDimensionsAndPosition(120,20,width/2 - 60,height - 30);

        addDrawable(categoryTitle);
        addDrawable(questionTitle);
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

    @Override
    public void tick() {
        categoryDeleteButton.active = !(categoryList.getSelectedOrNull() == null);
        categoryRenameButton.active = !(categoryList.getSelectedOrNull() == null);

        questionEditButton.active = !(questionList.getSelectedOrNull() == null);
        questionDeleteButton.active = !(questionList.getSelectedOrNull() == null);

        applyButton.active = !categoryList.children().isEmpty();
        questionAddButton.active = !(categoryList.getSelectedOrNull() == null);
    }

}

