package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

class AdditionalConfigScreen extends Screen {
    protected AdditionalConfigScreen() {
        super(Text.literal("Additional Config Screen"));
        parent = MinecraftClient.getInstance().currentScreen;
        correctAnswerEditBox.setText(config.correctAnswerCommands().stream().reduce((c1,c2) -> c1 + "\n" + c2).orElse(""));
        wrongAnswerEditBox.setText(config.wrongAnswerCommands().stream().reduce((c1,c2) -> c1 + "\n" + c2).orElse(""));
    }

    private final Screen parent;
    private final ModConfig config = FileManager.getConfig();
    private final ButtonWidget titleTooltip = ButtonWidget.builder(Text.literal("ℹ"),b -> {}).tooltip(Tooltip.of(Text.literal("These additional configurations only work if you are playing on an integrated server (e.g. Singleplayer,LAN world) OR if you have a permission level of 2 or greater on an external server."))).build();
    private final TextWidget title = new TextWidget(Text.literal("Additional Configuration"), MinecraftClient.getInstance().textRenderer);
    private final TextWidget title2 = new TextWidget(Text.literal("If answer is correct, run these commands:"),MinecraftClient.getInstance().textRenderer);
    private final TextWidget title3 = new TextWidget(Text.literal("If answer is wrong, run these commands:"),MinecraftClient.getInstance().textRenderer);
    private final EditBoxWidget correctAnswerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,350,50,Text.literal("Write your commands on seperate lines here, without the /"),Text.empty());
    private final EditBoxWidget wrongAnswerEditBox = new EditBoxWidget(MinecraftClient.getInstance().textRenderer,0,0,350,50,Text.literal("Write your commands on seperate lines here, without the /"),Text.empty());

    private final CyclingButtonWidget<String> commandSelectionButton = new CyclingButtonWidget.Builder<>(Text::literal)
            .values(Arrays.stream(ModConfig.CommandSelectionStrategy.values()).map(commandSelectionStrategy -> commandSelectionStrategy.friendlyName).toList())
            .initially(config.commandSelectionStrategy().friendlyName)
            .tooltip(value -> ModConfig.CommandSelectionStrategy.fromFriendlyName(value).tooltip)
            .build(0,0,0,0,Text.literal("Mode"));

    private final ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"),button -> {
        List<String> correctAnswerCommands = Arrays.asList(correctAnswerEditBox.getText().split("\n"));
        List<String> wrongAnswerCommands = Arrays.asList(wrongAnswerEditBox.getText().split("\n"));
        FileManager.updateConfig(new ModConfig(config.interval(),config.intervalToggle(),config.validationToggle(),config.categoryToggle(),correctAnswerCommands,wrongAnswerCommands, ModConfig.CommandSelectionStrategy.fromFriendlyName(commandSelectionButton.getValue())));
        this.close();
    }).build();

    @Override
    public void init() {
        title.alignCenter().setDimensionsAndPosition(width,10,0,10);
        titleTooltip.setDimensionsAndPosition(16,16,width/2 + 70,5);
        commandSelectionButton.setDimensionsAndPosition(130,20,width/2 - 65,25);
        title2.alignCenter().setDimensionsAndPosition(width,15,0,50);
        title3.alignCenter().setDimensionsAndPosition(width,15,0,140);
        correctAnswerEditBox.setDimensionsAndPosition(350,50,width/2-175,70);
        wrongAnswerEditBox.setDimensionsAndPosition(350,50,width/2-175,160);
        doneButton.setDimensionsAndPosition(100,20,width/2-47,240);

        addDrawable(title);
        addDrawable(titleTooltip);
        addDrawableChild(commandSelectionButton);
        addDrawable(title2);
        addDrawableChild(correctAnswerEditBox);
        addDrawable(title3);
        addDrawableChild(wrongAnswerEditBox);
        addDrawableChild(doneButton);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

}
