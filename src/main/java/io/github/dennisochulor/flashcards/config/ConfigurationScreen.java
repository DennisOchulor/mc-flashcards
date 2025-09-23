package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.ClientModInit;
import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class ConfigurationScreen extends Screen {

    private ModConfig config = FileManager.getConfig();
    private final TextFieldWidget intervalTextField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 30, 20,Text.empty());
    private final TextWidget titleText = new TextWidget(Text.literal("Flashcards Mod Config"), MinecraftClient.getInstance().textRenderer);
    private final TextWidget intervalText = new TextWidget(Text.literal("Prompt a question every          minutes"), MinecraftClient.getInstance().textRenderer);

    private final ButtonWidget intervalButton = ButtonWidget.builder(Text.literal(config.intervalToggle() ? "ON" : "OFF"), button -> {
        if (button.getMessage().getString().equals("ON")) button.setMessage(Text.literal("OFF"));
        else button.setMessage(Text.literal("ON"));
    }).build();
    private final ButtonWidget editButton = ButtonWidget.builder(Text.literal("Edit Questions"), button -> MinecraftClient.getInstance().setScreen(new EditScreen())).build();
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
        ModConfig newConfig = new ModConfig(Integer.parseInt(intervalTextField.getText()), intervalButton.getMessage().getString().equals("ON"),config.validationToggle(),config.categoryToggle(),config.correctAnswerCommands(),config.wrongAnswerCommands(),config.commandSelectionStrategy());
        if(newConfig.equals(config)) {
            this.close();
            return;
        }

        FileManager.updateConfig(newConfig);
        QuestionScheduler.updateConfig(newConfig);
        this.close();
    }).build();
    private final ButtonWidget generalConfigButton = ButtonWidget.builder(Text.literal("General Config..."),button -> MinecraftClient.getInstance().setScreen(new GeneralConfigScreen())).build();
    private final ButtonWidget additionalConfigButton = ButtonWidget.builder(Text.literal("Additional Config..."), button -> MinecraftClient.getInstance().setScreen(new AdditionalConfigScreen())).build();
    private final ButtonWidget statsButton = ButtonWidget.builder(Text.literal("Stats"), button -> {
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
    }).build();

    private final ButtonWidget openFlashcardsFolderButton = ButtonWidget.builder(Text.literal("📂"),button -> {
        try {
            Desktop.getDesktop().open(new File(MinecraftClient.getInstance().runDirectory.toPath() + "/config/flashcards"));
        }
        catch (IOException e) {
            ClientModInit.LOGGER.warn("Failed to open flashcards folder",e);
        }
    }).tooltip(Tooltip.of(Text.literal("Open Flashcards Mod Folder"))).build();

    public ConfigurationScreen() {
        super(Text.literal("Flashcards Config"));
        intervalTextField.setText(String.valueOf(config.interval()));
        intervalTextField.setChangedListener(text -> {
            try {
                if(Integer.parseInt(text) < 1) throw new IllegalArgumentException();
                else doneButton.active = true;
            }
            catch (IllegalArgumentException e) {
                doneButton.active = false;
            }
        });
    }

    @Override
    protected void init() {
        config = FileManager.getConfig(); // avoid overriding config updates of nested screens
        titleText.setPosition(width/2 - titleText.getWidth()/2,15);
        intervalText.setPosition(width/2 - 150, 50);
        intervalButton.setDimensionsAndPosition(30,20,width/2 + 75, 40);
        intervalTextField.setPosition(width/2 - 20, 40);

        editButton.setDimensionsAndPosition(100,20,width/2 - 50,85);
        generalConfigButton.setDimensionsAndPosition(100,20,width/2 - 50,115);
        additionalConfigButton.setDimensionsAndPosition(100,20,width/2 - 50,145);
        statsButton.setDimensionsAndPosition(100,20,width/2 - 50,175);
        doneButton.setDimensionsAndPosition(75,20,width/2 - 37,height - 30);
        openFlashcardsFolderButton.setDimensionsAndPosition(20,20,width/2 + 55,85);

        addDrawable(titleText);
        addDrawable(intervalText);
        addDrawableChild(intervalTextField);
        addDrawableChild(intervalButton);
        addDrawableChild(editButton);
        addDrawableChild(generalConfigButton);
        addDrawableChild(additionalConfigButton);
        addDrawableChild(statsButton);
        addDrawableChild(doneButton);
        if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            addDrawableChild(openFlashcardsFolderButton);
        }
    }

}