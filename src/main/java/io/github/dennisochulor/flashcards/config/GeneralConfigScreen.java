package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

class GeneralConfigScreen extends Screen {
    protected GeneralConfigScreen() {
        super(Text.literal("General Config Screen"));
        parent = MinecraftClient.getInstance().currentScreen;
    }

    private final ModConfig config = FileManager.getConfig();
    private final Screen parent;
    private final TextWidget titleText = new TextWidget(Text.literal("General Config"), MinecraftClient.getInstance().textRenderer);
    private final CyclingButtonWidget<String> validationToggleButton = new CyclingButtonWidget.Builder<>(Text::literal)
            .values("Automatic","Manual").initially(config.validationToggle() ? "Automatic" : "Manual")
            .tooltip(value -> switch(value) {
                case "Automatic" -> Tooltip.of(Text.literal("Automatically validates your answers in a case insensitive manner."));
                case "Manual" -> Tooltip.of(Text.literal("Manually validate your answers yourself, useful if your questions have multiple acceptable answers."));
                default -> throw new IllegalArgumentException("Invalid validationToggle button value");
            }).build(width/2 - 50,30,100,20,Text.literal("Answer Validation Mode"));
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
        boolean validationToggle = validationToggleButton.getValue().equals("Automatic");

        ModConfig newConfig = new ModConfig(config.interval(),config.intervalToggle(),validationToggle,config.categoryToggle(),config.correctAnswerCommands(),config.wrongAnswerCommands());
        FileManager.updateConfig(newConfig);
        QuestionScheduler.updateConfig(newConfig);
        this.close();
    }).build();

    @Override
    protected void init() {
        titleText.alignCenter().setDimensionsAndPosition(width,10,0,15);
        validationToggleButton.setDimensionsAndPosition(200,20,width/2 - 100,50);
        doneButton.setDimensionsAndPosition(100,20,width/2 - 50,220);

        addDrawable(titleText);
        addDrawableChild(validationToggleButton);
        addDrawableChild(doneButton);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
