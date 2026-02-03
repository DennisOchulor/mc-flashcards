package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

class CategoryRenameScreen extends Screen {
    protected CategoryRenameScreen(String oldName) {
        super(Component.literal("Category Rename Screen"));
        this.title.setMessage(Component.literal("Rename Category '" + oldName + "'"));
        this.oldName = oldName;
    }

    private String oldName;
    private final EditScreen parent = (EditScreen) Objects.requireNonNull(Minecraft.getInstance().screen);
    private final StringWidget title = new StringWidget(Component.empty(), Minecraft.getInstance().font);
    private final StringWidget title2 = new StringWidget(Component.literal("New Category Name:"),Minecraft.getInstance().font);
    private final EditBox textField = new EditBox(Minecraft.getInstance().font, 100,10,Component.empty());
    private final StringWidget warningText = new StringWidget(Component.literal("A category with this name already exists!").withColor(CommonColors.RED),Minecraft.getInstance().font);
    private final Button doneButton = Button.builder(Component.literal("Done"),_ -> {
        String newName = textField.getValue();
        if (parent.categoriesMap.containsKey(newName))  addRenderableOnly(warningText);
        else {
            List<Question> list = parent.categoriesMap.get(oldName);
            parent.categoriesMap.remove(oldName);
            parent.categoriesMap.put(newName,list);
            Objects.requireNonNull(parent.categoryList.getSelected()).name = newName;
            this.onClose();
        }
    }).build();

    @Override
    public void init() {
        title.setPosition(width/2 - title.getWidth()/2,10);
        title2.setPosition(width/2 - title2.getWidth()/2,50);
        textField.setRectangle(100,20,width/2-50,70);
        textField.setMaxLength(10);
        textField.setResponder(text -> doneButton.active = !text.isBlank());
        doneButton.setRectangle(100,20,width/2-50,height - 30);
        doneButton.active = false;
        warningText.setPosition(width/2 - warningText.getWidth()/2,100);

        addRenderableOnly(title);
        addRenderableOnly(title2);
        addRenderableWidget(textField);
        addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

}
