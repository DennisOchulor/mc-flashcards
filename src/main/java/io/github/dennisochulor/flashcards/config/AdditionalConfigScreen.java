package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class AdditionalConfigScreen extends Screen {
    protected AdditionalConfigScreen() {
        super(Component.literal("Additional Config Screen"));
        parent = Objects.requireNonNull(Minecraft.getInstance().screen); // must be the ConfigurationScreen
        correctAnswerEditBox.setValue(config.correctAnswerCommands().stream().reduce((c1,c2) -> c1 + "\n" + c2).orElse(""));
        wrongAnswerEditBox.setValue(config.wrongAnswerCommands().stream().reduce((c1,c2) -> c1 + "\n" + c2).orElse(""));
    }

    private final Screen parent;
    private final ModConfig config = FileManager.getConfig();
    private final Button titleTooltip = Button.builder(Component.literal("ℹ"),_ -> {}).tooltip(Tooltip.create(Component.literal("The specified commands will fail if your player has insufficient permissions."))).build();
    private final StringWidget title = new StringWidget(Component.literal("Additional Configuration"), Minecraft.getInstance().font);
    private final StringWidget title2 = new StringWidget(Component.literal("If answer is correct, run these commands:"),Minecraft.getInstance().font);
    private final StringWidget title3 = new StringWidget(Component.literal("If answer is wrong, run these commands:"),Minecraft.getInstance().font);
    private final MultiLineEditBox correctAnswerEditBox = MultiLineEditBox.builder().setPlaceholder(Component.literal("Write your commands on seperate lines here, without the /")).build(Minecraft.getInstance().font, 350, 50, Component.empty());
    private final MultiLineEditBox wrongAnswerEditBox = MultiLineEditBox.builder().setPlaceholder(Component.literal("Write your commands on seperate lines here, without the /")).build(Minecraft.getInstance().font, 350, 50, Component.empty());

    private final CycleButton<ModConfig.CommandSelectionStrategy> commandSelectionButton =
            new CycleButton.Builder<>(strat -> Component.literal(strat.friendlyName), config::commandSelectionStrategy)
            .withValues(List.of(ModConfig.CommandSelectionStrategy.values()))
            .withTooltip(strat -> strat.tooltip)
            .create(0,0,0,0,Component.literal("Mode"));

    private final Button doneButton = Button.builder(Component.literal("Done"),_ -> {
        List<String> correctAnswerCommands = Arrays.asList(correctAnswerEditBox.getValue().split("\n"));
        List<String> wrongAnswerCommands = Arrays.asList(wrongAnswerEditBox.getValue().split("\n")); // withers i need you now!!!!
        FileManager.updateConfig(new ModConfig(config.interval(),config.intervalToggle(),config.validationToggle(),config.categoryToggle(),
                correctAnswerCommands,wrongAnswerCommands, commandSelectionButton.getValue()));
        this.onClose();
    }).build();

    @Override
    public void init() {
        title.setPosition(width/2 - title.getWidth()/2,10);
        titleTooltip.setRectangle(16,16,width/2 + 70,5);
        commandSelectionButton.setRectangle(130,20,width/2 - 65,25);

        title2.setPosition(width/2 - title2.getWidth()/2,70);
        correctAnswerEditBox.setRectangle(350,height/5,width/2-175, title2.getBottom() + 10);

        title3.setPosition(width/2 - title3.getWidth()/2, correctAnswerEditBox.getBottom() + 20);
        wrongAnswerEditBox.setRectangle(350,height/5,width/2-175, title3.getBottom() + 10);

        doneButton.setRectangle(100,20,width/2-47,height - 30);

        addRenderableOnly(title);
        addRenderableOnly(titleTooltip);
        addRenderableWidget(commandSelectionButton);
        addRenderableOnly(title2);
        addRenderableWidget(correctAnswerEditBox);
        addRenderableOnly(title3);
        addRenderableWidget(wrongAnswerEditBox);
        addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

}
