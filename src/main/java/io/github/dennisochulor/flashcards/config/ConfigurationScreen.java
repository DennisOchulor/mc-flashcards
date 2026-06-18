package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.Flashcards;
import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ConfigurationScreen extends Screen {

    private ModConfig config = FileManager.getConfig();
    @Nullable private final Screen parent;
    private final EditBox intervalTextField = new EditBox(Minecraft.getInstance().font, 30, 20,Component.empty());
    private final StringWidget titleText = new StringWidget(Component.literal("Flashcards Mod Config"), Minecraft.getInstance().font);
    private final StringWidget intervalText = new StringWidget(Component.literal("Prompt a question every          minutes"), Minecraft.getInstance().font);

    private final Button intervalButton = Button.builder(Component.literal(config.intervalToggle() ? "ON" : "OFF"), button -> {
        if (button.getMessage().getString().equals("ON")) button.setMessage(Component.literal("OFF"));
        else button.setMessage(Component.literal("ON"));
    }).build();
    private final Button editButton = Button.builder(Component.literal("Edit Questions"), _ -> Minecraft.getInstance().gui.setScreen(new EditScreen())).build();
    private final Button doneButton = Button.builder(Component.literal("Done"), _ -> this.onClose()).build();
    private final Button generalConfigButton = Button.builder(Component.literal("General Config..."),_ -> Minecraft.getInstance().gui.setScreen(new GeneralConfigScreen())).build();
    private final Button additionalConfigButton = Button.builder(Component.literal("Additional Config..."), _ -> Minecraft.getInstance().gui.setScreen(new AdditionalConfigScreen())).build();
    private final Button statsButton = Button.builder(Component.literal("Stats"), _ -> {
        ModStats stats = FileManager.getStats();
        float correctPercentage, wrongPercentage;
        if (stats.totalQuestionsAnswered() == 0) { // avoid DivideByZeroException
            correctPercentage = 0;
            wrongPercentage = 0;
        }
        else {
            correctPercentage = (float) stats.correctAnswers() / stats.totalQuestionsAnswered() * 100;
            wrongPercentage = (float) stats.wrongAnswers() / stats.totalQuestionsAnswered() * 100;
        }
        Component msg = Component.literal(String.format("Total questions answered: %d\nCorrect answers: %d (%.2f%%)\nWrong answers: %d (%.2f%%)",stats.totalQuestionsAnswered(),stats.correctAnswers(),correctPercentage,stats.wrongAnswers(),wrongPercentage));
        PopupScreen popup = new PopupScreen.Builder(this,Component.literal("Flashcards Mod Stats"))
                .addMessage(msg).addButton(Component.literal("Done"),PopupScreen::onClose).build();
        Minecraft.getInstance().gui.setScreen(popup);
    }).build();

    private final Button openFlashcardsFolderButton = Button.builder(Component.literal("📂"),_ -> {
        try {
            Desktop.getDesktop().open(new File(Minecraft.getInstance().gameDirectory.toPath() + "/config/flashcards"));
        }
        catch (IOException e) {
            Flashcards.LOGGER.warn("Failed to open flashcards folder",e);
        }
    }).tooltip(Tooltip.create(Component.literal("Open Flashcards Mod Folder"))).build();

    public ConfigurationScreen(@Nullable Screen parent) {
        super(Component.literal("Flashcards Config"));

        this.parent = parent;
        intervalTextField.setValue(String.valueOf(config.interval()));
        intervalTextField.setResponder(text -> {
            try {
                if (Integer.parseInt(text) < 1) throw new IllegalArgumentException();
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
        intervalButton.setRectangle(30,20,width/2 + 75, 40);
        intervalTextField.setPosition(width/2 - 20, 40);

        editButton.setRectangle(100,20,width/2 - 50,85);
        generalConfigButton.setRectangle(100,20,width/2 - 50,115);
        additionalConfigButton.setRectangle(100,20,width/2 - 50,145);
        statsButton.setRectangle(100,20,width/2 - 50,175);
        doneButton.setRectangle(75,20,width/2 - 37,height - 30);
        openFlashcardsFolderButton.setRectangle(20,20,width/2 + 55,85);

        addRenderableOnly(titleText);
        addRenderableOnly(intervalText);
        addRenderableWidget(intervalTextField);
        addRenderableWidget(intervalButton);
        addRenderableWidget(editButton);
        addRenderableWidget(generalConfigButton);
        addRenderableWidget(additionalConfigButton);
        addRenderableWidget(statsButton);
        addRenderableWidget(doneButton);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            addRenderableWidget(openFlashcardsFolderButton);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);

        ModConfig newConfig = new ModConfig(Integer.parseInt(intervalTextField.getValue()), intervalButton.getMessage().getString().equals("ON"),config.validationToggle(),config.categoryToggle(),config.correctAnswerCommands(),config.wrongAnswerCommands(),config.commandSelectionStrategy());
        if (!newConfig.equals(config)) {
            FileManager.updateConfig(newConfig);
            QuestionScheduler.updateConfig(newConfig);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return doneButton.active;
    }
}