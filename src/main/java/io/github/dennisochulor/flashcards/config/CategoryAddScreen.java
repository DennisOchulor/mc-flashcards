package io.github.dennisochulor.flashcards.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.ArrayList;

class CategoryAddScreen extends Screen {
    protected CategoryAddScreen() {
        super(Text.literal("Category Add Screen"));
    }

    private final EditScreen parent = (EditScreen) MinecraftClient.getInstance().currentScreen;
    private final TextWidget title = new TextWidget(Text.literal("Add New Category"), MinecraftClient.getInstance().textRenderer);
    private final TextWidget title2 = new TextWidget(Text.literal("New Category Name:"),MinecraftClient.getInstance().textRenderer);
    private final TextFieldWidget textField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 100,10,Text.empty());
    private final TextWidget warningText = new TextWidget(Text.literal("A category with this name already exists!"),MinecraftClient.getInstance().textRenderer);
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"),button -> {
        String newName = textField.getText();
        if(parent.categoriesMap.containsKey(newName))  addDrawable(warningText);
        else {
            parent.categoriesMap.put(newName,new ArrayList<>());
            CategoryListWidget.Entry e = new CategoryListWidget.Entry(newName,true);
            parent.categoryList.add(e);
            parent.questionList.setSelected(null);
            this.close();
            parent.categoryList.setSelected(e);
        }
    }).build();

    @Override
    public void init() {
        title.setPosition(width/2 - title.getWidth()/2,10);
        title2.setPosition(width/2 - title2.getWidth()/2,50);
        textField.setDimensionsAndPosition(100,20,width/2-50,70);
        textField.setMaxLength(10);
        textField.setChangedListener(text -> doneButton.active = !text.isBlank());
        doneButton.setDimensionsAndPosition(100,20,width/2-50,200);
        doneButton.active = false;
        warningText.setTextColor(Colors.RED);
        warningText.setDimensionsAndPosition(width,10,0,100);

        addDrawable(title);
        addDrawable(title2);
        addDrawableChild(textField);
        addDrawableChild(doneButton);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

}
