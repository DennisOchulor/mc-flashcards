package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

class CategoryEditScreen extends Screen {
    private CategoryEditScreen(@Nullable String oldName, DoneButtonAction successAction) {
        Component titleText = Component.literal(oldName == null ? "Add new category" : "Rename " + oldName);

        super(titleText);
        this.title.setMessage(titleText);
        
        this.doneButton = Button.builder(Component.literal("Done"),_ -> {
            String newName = textField.getValue();
            if (parent.categoriesMap.containsKey(newName)) warningText.visible = true;
            else {
                successAction.onDoneButtonPressed(parent, oldName, newName);
                this.onClose();
            }

        }).build();
    }

    private final EditScreen parent = (EditScreen) Objects.requireNonNull(Minecraft.getInstance().gui.screen());
    private final StringWidget title = new StringWidget(Component.empty(), Minecraft.getInstance().font);
    private final StringWidget title2 = new StringWidget(Component.literal("New Category Name:"),Minecraft.getInstance().font);
    private final EditBox textField = new EditBox(Minecraft.getInstance().font, 100,20,Component.empty());
    private final StringWidget warningText = new StringWidget(Component.literal("A category with this name already exists!").withColor(CommonColors.RED),Minecraft.getInstance().font);
    private final Button doneButton;

    @Override
    public void init() {
        textField.setMaxLength(10);
        textField.setResponder(text -> doneButton.active = !text.isBlank());
        warningText.visible = false;
        doneButton.active = !textField.getValue().isBlank();

        LinearLayout contents = LinearLayout.vertical().spacing(10);
        contents.defaultCellSetting().alignHorizontallyCenter();
        contents.addChild(title2);
        contents.addChild(textField);
        contents.addChild(warningText);

        HeaderAndFooterLayout root = new HeaderAndFooterLayout(this, 20, 40);
        root.addToHeader(title);
        root.addToContents(contents);
        root.addToFooter(doneButton);

        root.arrangeElements();
        FrameLayout.alignInRectangle(root, 0, 0, this.width, this.height, 0.5F, 0.1F);
        root.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }


    @FunctionalInterface
    private interface DoneButtonAction {
        void onDoneButtonPressed(EditScreen parent, @Nullable String oldName, String newName);
    }

    public static CategoryEditScreen newCategory() {
        return new CategoryEditScreen(null, (parent, _, newName) -> {
            parent.categoriesMap.put(newName,new ArrayList<>());
            CategoryListWidget.Entry e = new CategoryListWidget.Entry(newName,true);
            parent.categoryList.add(e);
            parent.categoryList.setSelected(e);
            parent.questionList.setSelected(null);
            parent.questionList.changeList(List.of());
        });
    }

    public static CategoryEditScreen renameCategory(String originalName) {
        return new CategoryEditScreen(originalName, ((parent, oldName, newName) -> {
            List<Question> list = parent.categoriesMap.get(oldName);
            parent.categoriesMap.remove(oldName);
            parent.categoriesMap.put(newName,list);
            Objects.requireNonNull(parent.categoryList.getSelected()).name = newName;
        }));
    }

}
