package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class AdditionalConfigScreen extends Screen {
    protected AdditionalConfigScreen() {
        super(Component.literal("Additional Config Screen"));
        parent = Objects.requireNonNull(Minecraft.getInstance().gui.screen()); // must be the ConfigurationScreen
        correctAnswerEditBox.setValue(config.correctAnswerCommands().stream().reduce((c1,c2) -> c1 + "\n" + c2).orElse(""));
        wrongAnswerEditBox.setValue(config.wrongAnswerCommands().stream().reduce((c1,c2) -> c1 + "\n" + c2).orElse(""));
    }

    private final Screen parent;
    private final ModConfig config = FileManager.getConfig();

    private final Button titleTooltip = Button.builder(Component.literal("ℹ"),_ -> {})
            .tooltip(Tooltip.create(Component.literal("The specified commands will fail if your player has insufficient permissions.")))
            .size(16, 16).build();

    private final StringWidget title = new StringWidget(Component.literal("Additional Configuration"), Minecraft.getInstance().font);
    private final StringWidget title2 = new StringWidget(Component.literal("If answer is correct, run these commands:"),Minecraft.getInstance().font);
    private final StringWidget title3 = new StringWidget(Component.literal("If answer is wrong, run these commands:"),Minecraft.getInstance().font);

    private final MultiLineEditBox correctAnswerEditBox = MultiLineEditBox.builder().setPlaceholder(Component.literal("Write your commands on seperate lines here, without the /"))
            .build(Minecraft.getInstance().font, 350, 50, Component.empty());
    private final MultiLineEditBox wrongAnswerEditBox = MultiLineEditBox.builder().setPlaceholder(Component.literal("Write your commands on seperate lines here, without the /"))
            .build(Minecraft.getInstance().font, 350, 50, Component.empty());

    private final CycleButton<ModConfig.CommandSelectionStrategy> commandSelectionButton =
            new CycleButton.Builder<>(strat -> Component.literal(strat.friendlyName), config::commandSelectionStrategy)
            .withValues(List.of(ModConfig.CommandSelectionStrategy.values()))
            .withTooltip(strat -> strat.tooltip)
            .create(0,0,130,20,Component.literal("Mode"));

    private final Button doneButton = Button.builder(Component.literal("Done"),_ -> this.onClose())
            .size(100, 20).build();

    @Override
    public void init() {
        LinearLayout titleLayout = LinearLayout.horizontal().spacing(10);
        titleLayout.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        titleLayout.addChild(title);
        titleLayout.addChild(titleTooltip);

        int editBoxHeight = Math.max(50, (int) (this.height * 0.2));
        correctAnswerEditBox.setHeight(editBoxHeight);
        wrongAnswerEditBox.setHeight(editBoxHeight);

        LinearLayout contentLayout = LinearLayout.vertical().spacing(5);
        contentLayout.defaultCellSetting().alignHorizontallyCenter();
        contentLayout.addChild(commandSelectionButton, contentLayout.newCellSettings().paddingBottom(5));
        contentLayout.addChild(title2);
        contentLayout.addChild(correctAnswerEditBox, contentLayout.newCellSettings().paddingBottom(5));
        contentLayout.addChild(title3);
        contentLayout.addChild(wrongAnswerEditBox);

        HeaderAndFooterLayout root = new HeaderAndFooterLayout(this, 25, 40);
        root.addToHeader(titleLayout);
        root.addToContents(contentLayout);
        root.addToFooter(doneButton);

        root.arrangeElements();
        FrameLayout.alignInRectangle(root, 0, 0, this.width, this.height, 0.5F, 0.1F);
        root.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose() {
        List<String> correctAnswerCommands = Arrays.asList(correctAnswerEditBox.getValue().split("\n"));
        List<String> wrongAnswerCommands = Arrays.asList(wrongAnswerEditBox.getValue().split("\n")); // withers i need you now!!!!
        FileManager.updateConfig(new ModConfig(config.interval(),config.intervalToggle(),config.validationToggle(),config.categoryToggle(),
                correctAnswerCommands,wrongAnswerCommands, commandSelectionButton.getValue()));

        Minecraft.getInstance().gui.setScreen(parent);
    }

}
