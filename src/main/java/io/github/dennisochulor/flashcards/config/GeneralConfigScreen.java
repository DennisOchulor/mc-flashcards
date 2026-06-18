package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

class GeneralConfigScreen extends Screen {
    protected GeneralConfigScreen() {
        super(Component.literal("General Config Screen"));
        parent = Objects.requireNonNull(Minecraft.getInstance().gui.screen());
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
            }).create(0,0,200,20,Component.literal("Answer Validation Mode"));

    private final Button doneButton = Button.builder(Component.literal("Done"), _ -> this.onClose())
            .size(100, 20).build();

    @Override
    protected void init() {
        LinearLayout options = LinearLayout.vertical().spacing(10);
        options.defaultCellSetting().alignHorizontallyCenter();
        options.addChild(validationToggleButton);

        HeaderAndFooterLayout root = new HeaderAndFooterLayout(this, 25, 40);
        root.addToHeader(titleText);
        root.addToContents(options);
        root.addToFooter(doneButton);

        root.arrangeElements();
        FrameLayout.alignInRectangle(root, 0, 0, this.width, this.height, 0.5F, 0.1F);
        root.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose() {
        boolean validationToggle = validationToggleButton.getValue().equals("Automatic");

        ModConfig newConfig = new ModConfig(config.interval(),config.intervalToggle(),validationToggle,config.categoryToggle(),config.correctAnswerCommands(),config.wrongAnswerCommands(),config.commandSelectionStrategy());
        FileManager.updateConfig(newConfig);
        QuestionScheduler.updateConfig(newConfig);

        Minecraft.getInstance().gui.setScreen(parent);
    }
}
