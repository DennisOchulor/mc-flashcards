package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.Question;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class EditScreen extends Screen {

    protected EditScreen() {
        super(Component.literal("Edit screen"));
        parent = Objects.requireNonNull(Minecraft.getInstance().screen);
    }

    private final Screen parent;
    final HashMap<String,List<Question>> categoriesMap = FileManager.getQuestions();
    private final ModConfig config = FileManager.getConfig();
    final CategoryListWidget categoryList = new CategoryListWidget(categoriesMap,config.categoryToggle());
    final QuestionListWidget questionList = new QuestionListWidget(categoriesMap.get(Objects.requireNonNull(categoryList.getSelected()).name));
    private final StringWidget categoryTitle = new StringWidget(Component.literal("Categories"),Minecraft.getInstance().font);
    private final StringWidget questionTitle = new StringWidget(Component.literal("Questions"),Minecraft.getInstance().font);

    private final Button categoryRenameButton = Button.builder(Component.literal("Rename"),_ -> {
        Objects.requireNonNull(categoryList.getSelected());
        Minecraft.getInstance().setScreen(new CategoryRenameScreen(categoryList.getSelected().name));
    }).build();
    private final Button categoryAddButton = Button.builder(Component.literal("Add"), _ -> Minecraft.getInstance().setScreen(new CategoryAddScreen())).build();
    private final Button categoryDeleteButton = Button.builder(Component.literal("Delete"), _ -> {
        Objects.requireNonNull(categoryList.getSelected());
        categoriesMap.remove(categoryList.getSelected().name);
        categoryList.remove(categoryList.getSelected());
        categoryList.setSelected(null);
        questionList.changeList(List.of()); // clear
        questionList.setSelected(null);
    }).build();

    private final Button questionEditButton = Button.builder(Component.literal("Edit"), _ -> {
        Objects.requireNonNull(categoryList.getSelected());
        Objects.requireNonNull(questionList.getSelected());
        Minecraft.getInstance().setScreen(new QuestionEditScreen(questionList.getSelected(),categoryList.getSelected().name));
    }).build();
    private final Button questionDeleteButton = Button.builder(Component.literal("Delete"), _ -> {
        Objects.requireNonNull(questionList.getSelected());
        Objects.requireNonNull(categoryList.getSelected());
        categoriesMap.get(categoryList.getSelected().name).remove(questionList.getSelected().question);
        questionList.remove(questionList.getSelected());
        questionList.setSelected(null);
    }).build();
    private final Button questionAddButton = Button.builder(Component.literal("Add"), _ -> {
        Objects.requireNonNull(categoryList.getSelected());
        Minecraft.getInstance().setScreen(new QuestionAddScreen(categoryList.getSelected().name));
    }).build();

    private final Button applyButton = Button.builder(Component.literal("Apply Changes"), _ -> {
        FileManager.updateQuestions(categoriesMap);

        HashMap<String,Boolean> categoryToggle = new HashMap<>();
        for (var category : categoryList.children()) {
            categoryToggle.put(category.name,category.enabled);
        }

        ModConfig newConfig = new ModConfig(config.interval(),config.intervalToggle(),config.validationToggle(),categoryToggle,config.correctAnswerCommands(),config.wrongAnswerCommands(),config.commandSelectionStrategy());
        FileManager.updateConfig(newConfig);
        QuestionScheduler.reload();
        QuestionScheduler.updateConfig(newConfig);
        this.onClose();
    }).build();

    @Override
    protected void init() {
        categoryList.updateSizeAndPosition(85, 100, width/4 - 100, 20);
        questionList.updateSizeAndPosition(width/2, height - 75, width/2 - width/4, 20);

        categoryTitle.setPosition(categoryList.getWidth()/2 + categoryList.getX() - categoryTitle.getWidth()/2, categoryList.getY() - 15);
        questionTitle.setPosition(questionList.getWidth()/2 + questionList.getX() - questionTitle.getWidth()/2, questionList.getY() - 15);

        int categoryButtonsX = categoryList.getX() + categoryList.getWidth()/2 - 30;
        categoryRenameButton.setRectangle(60,20,categoryButtonsX,categoryList.getY() + categoryList.getHeight() + 20);
        categoryAddButton.setRectangle(60,20,categoryButtonsX,categoryRenameButton.getY() + 30);
        categoryDeleteButton.setRectangle(60,20,categoryButtonsX,categoryAddButton.getY() + 30);

        int questionButtonsX = questionList.getX() + questionList.getWidth() + 20;
        int questionButtonsMiddleY = questionList.getY() + questionList.getHeight()/2 - 10;
        questionEditButton.setRectangle(60,20,questionButtonsX,questionButtonsMiddleY - 30);
        questionAddButton.setRectangle(60,20,questionButtonsX,questionButtonsMiddleY);
        questionDeleteButton.setRectangle(60,20,questionButtonsX,questionButtonsMiddleY + 30);

        applyButton.setRectangle(120,20,width/2 - 60,height - 30);

        addRenderableOnly(categoryTitle);
        addRenderableOnly(questionTitle);
        addRenderableWidget(categoryList);
        addRenderableWidget(questionList);
        addRenderableWidget(categoryRenameButton);
        addRenderableWidget(categoryAddButton);
        addRenderableWidget(categoryDeleteButton);
        addRenderableWidget(questionEditButton);
        addRenderableWidget(questionAddButton);
        addRenderableWidget(questionDeleteButton);
        addRenderableWidget(applyButton);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void tick() {
        categoryDeleteButton.active = !(categoryList.getSelected() == null);
        categoryRenameButton.active = !(categoryList.getSelected() == null);

        questionEditButton.active = !(questionList.getSelected() == null);
        questionDeleteButton.active = !(questionList.getSelected() == null);

        applyButton.active = !categoryList.children().isEmpty();
        questionAddButton.active = !(categoryList.getSelected() == null);
    }

}

