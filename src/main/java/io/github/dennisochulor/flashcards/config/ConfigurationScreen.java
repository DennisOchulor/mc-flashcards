package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.ClientModInit;
import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.session.telemetry.WorldLoadedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.io.File;

@Environment(EnvType.CLIENT)
public class ConfigurationScreen extends Screen {

    public ConfigurationScreen() {
        super(Text.literal("Flashcards Config"));
    }

    private ButtonWidget intervalButton;
    private ButtonWidget reloadButton;
    private ButtonWidget doneButton;
    private TextFieldWidget intervalTextField;
    private TextWidget titleText;
    private TextWidget intervalText;

    @Override
    protected void init() {
        ModConfig config = FileManager.getConfig();

        titleText = new TextWidget(Text.literal("Flashcards Mod Config"), MinecraftClient.getInstance().textRenderer);
        titleText.setPosition(width/2 - 50, 15);
        intervalText = new TextWidget(Text.literal("Prompt a question every          minutes"), MinecraftClient.getInstance().textRenderer);
        intervalText.setPosition(width/2 - 150, 50);

        intervalButton = ButtonWidget.builder(Text.literal(config.intervalToggle() ? "ON" : "OFF"), button -> {
            if (button.getMessage().getString().equals("ON")) {
                button.setMessage(Text.literal("OFF"));
            }
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

        reloadButton = ButtonWidget.builder(Text.literal("Reload"), button -> QuestionScheduler.reload()).dimensions(width/2 - 35,85,75,20).build();

        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            ModConfig newConfig = new ModConfig(Integer.parseInt(intervalTextField.getText()), intervalButton.getMessage().getString().equals("ON"));
            if(newConfig.equals(config)) {
                this.close();
                return;
            }

            FileManager.updateConfig(newConfig);
            QuestionScheduler.updateConfig(newConfig);
            this.close();
        }).dimensions(width/2 - 35,200,75,20).build();

        addDrawable(titleText);
        addDrawable(intervalText);
        addDrawableChild(intervalTextField);
        addDrawableChild(intervalButton);
        addDrawableChild(reloadButton);
        addDrawableChild(doneButton);
    }

}