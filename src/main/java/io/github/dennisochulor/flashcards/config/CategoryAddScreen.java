package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.questions.Question;
import io.github.dennisochulor.flashcards.questions.QuestionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.ArrayList;
import java.util.List;

class CategoryAddScreen extends Screen {
    protected CategoryAddScreen() {
        super(Text.literal("Category Add Screen"));
        this.title.setMessage(Text.literal("Add New Category"));
    }

    private final EditScreen parent = (EditScreen) MinecraftClient.getInstance().currentScreen;
    private final TextWidget title = new TextWidget(Text.empty(), MinecraftClient.getInstance().textRenderer);
    private final TextWidget title2 = new TextWidget(Text.literal("New Category Name:"),MinecraftClient.getInstance().textRenderer);
    private final TextFieldWidget textField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 100,10,Text.empty());
    private final TextWidget warningText = new TextWidget(Text.literal("A category with this name already exists!"),MinecraftClient.getInstance().textRenderer);
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"),button -> {
        String newName = textField.getText();
        if(parent.map.containsKey(newName))  addDrawable(warningText);
        else {
            parent.map.put(newName,new ArrayList<>());
            CategoryListWidget.Entry e = new CategoryListWidget.Entry(newName,true);
            parent.categoryList.children().add(e);
            parent.questionList.setSelected(null);
            this.close();
            e.mouseClicked(0,0,0);
            parent.categoryList.setSelected(e);
        }
    }).build();

    @Override
    public void init() {
        title.alignCenter().setDimensionsAndPosition(150,30,width/2-65,10);
        title2.alignCenter().setDimensionsAndPosition(100,15,width/2-40,50);
        textField.setDimensionsAndPosition(100,20,width/2-42,70);
        textField.setMaxLength(10);
        textField.setChangedListener(text -> doneButton.active = !text.isBlank());
        doneButton.setDimensionsAndPosition(100,20,width/2-45,200);
        doneButton.active = false;
        warningText.setTextColor(Colors.RED);
        warningText.setPosition(width/2-90,100);

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
