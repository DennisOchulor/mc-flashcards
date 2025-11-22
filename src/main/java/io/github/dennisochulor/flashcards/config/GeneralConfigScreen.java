package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

class GeneralConfigScreen extends Screen {
    protected GeneralConfigScreen() {
        super(Component.literal("General Config Screen"));
        parent = Minecraft.getInstance().screen;
    }

    private final ModConfig config = FileManager.getConfig();
    private final Screen parent;
    private final StringWidget titleText = new StringWidget(Component.literal("General Config"), Minecraft.getInstance().font);
    private final CycleButton<String> validationToggleButton = new CycleButton.Builder<>(Component::literal, () -> config.validationToggle() ? "Automatic" : "Manual")
            .withValues("Automatic", "Manual")
            .withTooltip(value -> switch(value) {
                case "Automatic" -> Tooltip.create(Component.literal("Automatically validates your answers in a case insensitive manner."));
                case "Manual" -> Tooltip.create(Component.literal("Manually validate your answers yourself, useful if your questions have multiple acceptable answers."));
                default -> throw new IllegalArgumentException("Invalid validationToggle button value");
            }).create(width/2 - 50,30,100,20,Component.literal("Answer Validation Mode"));
    private final Button doneButton = Button.builder(Component.literal("Done"), button -> {
        boolean validationToggle = validationToggleButton.getValue().equals("Automatic");

        ModConfig newConfig = new ModConfig(config.interval(),config.intervalToggle(),validationToggle,config.categoryToggle(),config.correctAnswerCommands(),config.wrongAnswerCommands(),config.commandSelectionStrategy());
        FileManager.updateConfig(newConfig);
        QuestionScheduler.updateConfig(newConfig);
        this.onClose();
    }).build();

    @Override
    protected void init() {
        titleText.setPosition(width/2 - titleText.getWidth()/2,15);
        validationToggleButton.setRectangle(200,20,width/2 - 100,50);
        doneButton.setRectangle(100,20,width/2 - 50,height - 30);

        addRenderableOnly(titleText);
        addRenderableWidget(validationToggleButton);
        addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
