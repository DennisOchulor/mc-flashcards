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
    private final EditBox intervalTextField = new EditBox(Minecraft.getInstance().font, 30, 20,Component.literal("Minutes"));
    private final EditBox intervalSecondsField = new EditBox(Minecraft.getInstance().font, 30, 20,Component.literal("Seconds"));
    private final StringWidget titleText = new StringWidget(Component.literal("Flashcards: Anki + AI Edition"), Minecraft.getInstance().font);
    private final StringWidget intervalText = new StringWidget(Component.literal("Prompt a question every"), Minecraft.getInstance().font);
    private final StringWidget minutesText = new StringWidget(Component.literal("min"), Minecraft.getInstance().font);
    private final StringWidget secondsText = new StringWidget(Component.literal("sec"), Minecraft.getInstance().font);

    private final Button intervalButton = Button.builder(Component.literal(config.intervalToggle() ? "ON" : "OFF"), button -> {
        if (button.getMessage().getString().equals("ON")) button.setMessage(Component.literal("OFF"));
        else button.setMessage(Component.literal("ON"));
    }).build();
    private final Button editButton = Button.builder(Component.literal("Edit Questions"), _ -> Minecraft.getInstance().gui.setScreen(new EditScreen())).build();
    private final Button doneButton = Button.builder(Component.literal("Done"), _ -> this.onClose()).build();
    private final Button generalConfigButton = Button.builder(Component.literal("General Config..."),_ -> Minecraft.getInstance().gui.setScreen(new GeneralConfigScreen())).build();
    private final Button additionalConfigButton = Button.builder(Component.literal("Additional Config..."), _ -> Minecraft.getInstance().gui.setScreen(new AdditionalConfigScreen())).build();
    private final Button ankiConfigButton = Button.builder(Component.literal("Anki Sync..."), _ -> Minecraft.getInstance().gui.setScreen(new AnkiConfigScreen()))
            .tooltip(Tooltip.create(Component.literal("Study your real Anki deck in-game"))).build();
    private final Button aiGradingButton = Button.builder(Component.literal("AI Grading..."), _ -> Minecraft.getInstance().gui.setScreen(new AiGradingConfigScreen()))
            .tooltip(Tooltip.create(Component.literal("Type your answers and let a local AI grade them (Anki mode)"))).build();
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
        intervalSecondsField.setValue(String.valueOf(config.intervalSeconds()));
        intervalTextField.setMaxLength(4);
        intervalSecondsField.setMaxLength(2);
        intervalTextField.setTooltip(Tooltip.create(Component.literal("Minutes")));
        intervalSecondsField.setTooltip(Tooltip.create(Component.literal("Seconds (0-59)")));
        intervalTextField.setResponder(_ -> validateInterval());
        intervalSecondsField.setResponder(_ -> validateInterval());
    }

    /** @return {minutes, seconds}, or null if the fields don't make a valid interval (at least 1 second) */
    private int @Nullable [] parseInterval() {
        try {
            int minutes = intervalTextField.getValue().isBlank() ? 0 : Integer.parseInt(intervalTextField.getValue().trim());
            int seconds = intervalSecondsField.getValue().isBlank() ? 0 : Integer.parseInt(intervalSecondsField.getValue().trim());
            if (minutes < 0 || seconds < 0 || seconds > 59 || minutes * 60 + seconds < 1) return null;
            return new int[] {minutes, seconds};
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateInterval() {
        doneButton.active = parseInterval() != null;
    }

    @Override
    protected void init() {
        config = FileManager.getConfig(); // avoid overriding config updates of nested screens
        titleText.setPosition(width/2 - titleText.getWidth()/2,15);
        // "Prompt a question every [3] min [0] sec [ON]", centered
        int rowWidth = intervalText.getWidth() + 5 + 30 + 4 + minutesText.getWidth() + 8 + 30 + 4 + secondsText.getWidth() + 10 + 30;
        int x = width/2 - rowWidth/2;
        intervalText.setPosition(x, 46);
        x += intervalText.getWidth() + 5;
        intervalTextField.setPosition(x, 40);
        x += 30 + 4;
        minutesText.setPosition(x, 46);
        x += minutesText.getWidth() + 8;
        intervalSecondsField.setPosition(x, 40);
        x += 30 + 4;
        secondsText.setPosition(x, 46);
        x += secondsText.getWidth() + 10;
        intervalButton.setRectangle(30,20,x, 40);

        editButton.setRectangle(100,20,width/2 - 50,80);
        generalConfigButton.setRectangle(100,20,width/2 - 50,105);
        additionalConfigButton.setRectangle(100,20,width/2 - 50,130);
        ankiConfigButton.setRectangle(100,20,width/2 - 50,155);
        aiGradingButton.setRectangle(100,20,width/2 - 50,180);
        statsButton.setRectangle(100,20,width/2 - 50,205);
        doneButton.setRectangle(75,20,width/2 - 37,height - 30);
        openFlashcardsFolderButton.setRectangle(20,20,width/2 + 55,80);

        addRenderableOnly(titleText);
        addRenderableOnly(intervalText);
        addRenderableOnly(minutesText);
        addRenderableOnly(secondsText);
        addRenderableWidget(intervalTextField);
        addRenderableWidget(intervalSecondsField);
        addRenderableWidget(intervalButton);
        addRenderableWidget(editButton);
        addRenderableWidget(generalConfigButton);
        addRenderableWidget(additionalConfigButton);
        addRenderableWidget(ankiConfigButton);
        addRenderableWidget(aiGradingButton);
        addRenderableWidget(statsButton);
        addRenderableWidget(doneButton);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            addRenderableWidget(openFlashcardsFolderButton);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);

        int[] interval = parseInterval();
        if (interval == null) return; // can't happen: Done/Esc are disabled while the interval is invalid
        ModConfig newConfig = config.withInterval(interval[0], interval[1], intervalButton.getMessage().getString().equals("ON"));
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