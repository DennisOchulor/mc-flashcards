package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ConfigurationScreen extends Screen {

    public ConfigurationScreen() {
        super(Text.literal("Flashcards Config"));
    }

    private ButtonWidget intervalButton;
    private ButtonWidget editButton;
    private ButtonWidget doneButton;
    private ButtonWidget additionalConfigButton;
    private ButtonWidget statsButton;
    private TextFieldWidget intervalTextField;
    private TextWidget titleText;
    private TextWidget intervalText;

    @Override
    protected void init() {
        ModConfig config = FileManager.getConfig();

        titleText = new TextWidget(Text.literal("Flashcards Mod Config"), MinecraftClient.getInstance().textRenderer);
        titleText.alignCenter().setDimensionsAndPosition(width,10,0,15);
        intervalText = new TextWidget(Text.literal("Prompt a question every          minutes"), MinecraftClient.getInstance().textRenderer);
        intervalText.setPosition(width/2 - 150, 50);

        intervalButton = ButtonWidget.builder(Text.literal(config.intervalToggle() ? "ON" : "OFF"), button -> {
            if (button.getMessage().getString().equals("ON")) button.setMessage(Text.literal("OFF"));
            else button.setMessage(Text.literal("ON"));
        }).dimensions(width/2 + 75, 40, 30, 20).build();

        intervalTextField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 30, 20,Text.empty());
        intervalTextField.setText(String.valueOf(config.interval()));
        intervalTextField.setPosition(width/2 - 20, 40);
        intervalTextField.setChangedListener(text -> {
            try {
                if(Integer.parseInt(text) < 1) throw new IllegalArgumentException();
                else doneButton.active = true;
            }
            catch (IllegalArgumentException e) {
                doneButton.active = false;
            }
        });

        editButton = ButtonWidget.builder(Text.literal("Edit Questions"), button -> MinecraftClient.getInstance().setScreen(new EditScreen())).dimensions(width/2 - 50,85,100,20).build();

        additionalConfigButton = ButtonWidget.builder(Text.literal("Additional Config..."), button -> MinecraftClient.getInstance().setScreen(new AdditionalConfigScreen())).dimensions(width/2 - 50,120,100,20).build();

        statsButton = ButtonWidget.builder(Text.literal("Stats"), button -> {
            ModStats stats = FileManager.getStats();
            float correctPercentage, wrongPercentage;
            if(stats.totalQuestionsAnswered() == 0) { // avoid DivideByZeroException
                correctPercentage = 0;
                wrongPercentage = 0;
            }
            else {
                correctPercentage = (float) stats.correctAnswers() / stats.totalQuestionsAnswered() * 100;
                wrongPercentage = (float) stats.wrongAnswers() / stats.totalQuestionsAnswered() * 100;
            }
            Text msg = Text.literal(String.format("Total questions answered: %d\nCorrect answers: %d (%.2f%%)\nWrong answers: %d (%.2f%%)",stats.totalQuestionsAnswered(),stats.correctAnswers(),correctPercentage,stats.wrongAnswers(),wrongPercentage));
            PopupScreen popup = new PopupScreen.Builder(this,Text.literal("Flashcards Mod Stats")).message(msg).button(Text.literal("Done"),PopupScreen::close).build();
            MinecraftClient.getInstance().setScreen(popup);
        }).dimensions(width/2 - 50,155,100,20).build();

        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            ModConfig newConfig = new ModConfig(Integer.parseInt(intervalTextField.getText()), intervalButton.getMessage().getString().equals("ON"),config.categoryToggle(),config.correctAnswerCommands(),config.wrongAnswerCommands());
            if(newConfig.equals(config)) {
                this.close();
                return;
            }

            FileManager.updateConfig(newConfig);
            QuestionScheduler.updateConfig(newConfig);
            this.close();
        }).dimensions(width/2 - 37,200,75,20).build();

        addDrawable(titleText);
        addDrawable(intervalText);
        addDrawableChild(intervalTextField);
        addDrawableChild(intervalButton);
        addDrawableChild(editButton);
        addDrawableChild(additionalConfigButton);
        addDrawableChild(statsButton);
        addDrawableChild(doneButton);
    }

}