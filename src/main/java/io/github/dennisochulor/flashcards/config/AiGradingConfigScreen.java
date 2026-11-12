package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.IntSlider;
import io.github.dennisochulor.flashcards.ScoreSlider;
import io.github.dennisochulor.flashcards.ai.AiGrader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.Objects;

class AiGradingConfigScreen extends Screen {
    private static final int WIDGET_WIDTH = 280;
    private static final int SLIDER_WIDTH = 82;
    private static final int GRAY = 0xFFAAAAAA;

    private final Screen parent;
    private final ModConfig.AiGrading ai = FileManager.getConfig().aiGrading();

    private final StringWidget titleText = new StringWidget(Component.literal("AI Grading"), Minecraft.getInstance().font);

    private final CycleButton<Boolean> enabledButton = CycleButton.onOffBuilder(ai.enabled())
            .withTooltip(_ -> Tooltip.create(Component.literal(
                    "ON: type your answer and a local AI (Ollama) scores it by meaning from 0 to 1.\n" +
                    "The score decides which of Anki's buttons you can press.\n" +
                    "Works with Anki cards only (Anki Sync...).")))
            .create(0, 0, WIDGET_WIDTH, 20, Component.literal("AI grading"));

    private final EditBox urlBox = new EditBox(Minecraft.getInstance().font, WIDGET_WIDTH - 75, 20, Component.literal("Ollama address"));
    private final Button connectButton = Button.builder(Component.literal("Connect"), _ -> loadModels())
            .tooltip(Tooltip.create(Component.literal("Load your installed models from Ollama")))
            .size(70, 20).build();
    private final MultiLineTextWidget statusText = new MultiLineTextWidget(Component.empty(), Minecraft.getInstance().font);
    private final StringWidget modelLabel = new StringWidget(Component.empty(), Minecraft.getInstance().font);
    private final DeckListWidget modelList;

    private final StringWidget cutoffLabel = new StringWidget(
            Component.literal("Score needed for each button (Again: always)").withColor(GRAY), Minecraft.getInstance().font);
    // not final: each slider's listener refers to the others
    private ScoreSlider hardSlider;
    private ScoreSlider goodSlider;
    private ScoreSlider easySlider;
    private final Button resetButton = Button.builder(Component.literal("↺"), _ -> resetCutoffs())
            .tooltip(Tooltip.create(Component.literal("Reset to Hard 0.50, Good 0.70, Easy 0.90")))
            .size(20, 20).build();

    private final CycleButton<Boolean> adjustButton = CycleButton.onOffBuilder(ai.adjustScore())
            .withTooltip(_ -> Tooltip.create(Component.literal(
                    "ON: after the AI grades you, a slider lets you change the score if you disagree.\n" +
                    "OFF: the AI's score is final (you can still always press Again).")))
            .create(0, 0, WIDGET_WIDTH, 20, Component.literal("Adjusting score"));

    private final IntSlider quitAfterSlider = new IntSlider(WIDGET_WIDTH / 2 - 2, 20, 0, ModConfig.AiGrading.MAX_QUIT_AFTER_SECONDS,
            ai.quitAfterSeconds(), s -> Component.literal(s == 0 ? "Keep playing: at once" : "Keep playing after " + s + "s"));

    private final IntSlider minPlaySlider = new IntSlider(WIDGET_WIDTH / 2 - 2, 20, 0, ModConfig.AiGrading.MAX_MIN_PLAY_SECONDS,
            ai.minPlaySeconds(), s -> Component.literal(s == 0 ? "Play at least: 0s" : "Play at least " + s + "s"));

    private final CycleButton<Boolean> autoKeepPlayingButton = CycleButton.onOffBuilder(ai.autoKeepPlaying())
            .withTooltip(_ -> Tooltip.create(Component.literal(
                    "ON: as soon as you check an answer, you're back in the game while the AI thinks.\n" +
                    "OFF: you wait on the card, and \"Keep playing\" appears if it takes too long.")))
            .create(0, 0, WIDGET_WIDTH / 2 - 2, 20, Component.literal("Play while thinking"));

    private final CycleButton<ModConfig.IndicatorPosition> indicatorButton =
            new CycleButton.Builder<ModConfig.IndicatorPosition>(p -> Component.literal(p.friendlyName), ai::indicatorPosition)
            .withValues(ModConfig.IndicatorPosition.values())
            .withTooltip(_ -> Tooltip.create(Component.literal("Where the \"AI is thinking\" animation shows while you keep playing.")))
            .create(0, 0, WIDGET_WIDTH / 2 - 2, 20, Component.literal("Indicator"));

    private final MultiLineTextWidget helpText = new MultiLineTextWidget(Component.literal(
            "Needs the Ollama app running (ollama.com) with a model downloaded, e.g. \"ollama pull qwen2.5:7b\".")
            .withColor(GRAY), Minecraft.getInstance().font);

    private final Button doneButton = Button.builder(Component.literal("Done"), _ -> this.onClose()).size(100, 20).build();

    private String selectedModel = ai.model();
    private boolean initialized = false;

    protected AiGradingConfigScreen() {
        super(Component.literal("AI Grading Config"));
        parent = Objects.requireNonNull(Minecraft.getInstance().gui.screen());

        urlBox.setMaxLength(200);
        urlBox.setValue(ai.ollamaUrl());
        urlBox.setHint(Component.literal(ModConfig.DEFAULT_OLLAMA_URL));
        urlBox.setTooltip(Tooltip.create(Component.literal("Ollama address. The default is " + ModConfig.DEFAULT_OLLAMA_URL)));
        statusText.setMaxWidth(WIDGET_WIDTH).setCentered(true);
        helpText.setMaxWidth(WIDGET_WIDTH + 40).setCentered(true);

        modelList = new DeckListWidget(WIDGET_WIDTH, 100, 0, model -> {
            selectedModel = model;
            updateModelLabel();
        });

        hardSlider = new ScoreSlider(SLIDER_WIDTH, 20, "Hard ≥ ", ai.hardFrom(), v -> {
            if (goodSlider.score() < v) goodSlider.setScoreSilently(v);
            if (easySlider.score() < v) easySlider.setScoreSilently(v);
        });
        goodSlider = new ScoreSlider(SLIDER_WIDTH, 20, "Good ≥ ", ai.goodFrom(), v -> {
            if (hardSlider.score() > v) hardSlider.setScoreSilently(v);
            if (easySlider.score() < v) easySlider.setScoreSilently(v);
        });
        easySlider = new ScoreSlider(SLIDER_WIDTH, 20, "Easy ≥ ", ai.easyFrom(), v -> {
            if (goodSlider.score() > v) goodSlider.setScoreSilently(v);
            if (hardSlider.score() > v) hardSlider.setScoreSilently(v);
        });
        hardSlider.setTooltip(Tooltip.create(Component.literal("Lowest score that unlocks Hard")));
        goodSlider.setTooltip(Tooltip.create(Component.literal("Lowest score that unlocks Good")));
        easySlider.setTooltip(Tooltip.create(Component.literal("Lowest score that unlocks Easy")));

        quitAfterSlider.setTooltip(Tooltip.create(Component.literal(
                "If the AI thinks longer than this, a \"Keep playing\" button (or Space) lets you leave the card and play;\n" +
                "the card comes back when it's graded.")));
        minPlaySlider.setTooltip(Tooltip.create(Component.literal(
                "After \"Keep playing\", you get at least this long in the game before the card comes back.")));

        updateModelLabel();
        loadModels();
    }

    private String url() {
        String url = urlBox.getValue().trim();
        return url.isEmpty() ? ModConfig.DEFAULT_OLLAMA_URL : url;
    }

    private void resetCutoffs() {
        hardSlider.setScoreSilently(ModConfig.AiGrading.DEFAULT_HARD_FROM);
        goodSlider.setScoreSilently(ModConfig.AiGrading.DEFAULT_GOOD_FROM);
        easySlider.setScoreSilently(ModConfig.AiGrading.DEFAULT_EASY_FROM);
    }

    private void updateModelLabel() {
        modelLabel.setMessage(Component.literal("Model: ").withColor(GRAY)
                .append(selectedModel.isBlank() ? Component.literal("(none, pick one below)").withColor(CommonColors.SOFT_YELLOW)
                        : Component.literal(selectedModel).withColor(CommonColors.WHITE)));
        modelLabel.setX(width / 2 - modelLabel.getWidth() / 2); // re-center (no rebuild: this runs while the list handles a click)
    }

    private void loadModels() {
        connectButton.active = false;
        setStatus(Component.literal("Connecting to Ollama...").withColor(GRAY));

        AiGrader.loadModels(url(), models -> {
            connectButton.active = true;
            if (selectedModel.isBlank() && models.size() == 1) {
                selectedModel = models.getFirst();
                updateModelLabel();
            }
            modelList.setDecks(models, selectedModel);
            if (models.isEmpty()) {
                setStatus(Component.literal("Connected, but no models are installed. Run \"ollama pull qwen2.5:7b\".").withColor(CommonColors.SOFT_YELLOW));
            }
            else if (!selectedModel.isBlank() && !models.contains(selectedModel)) {
                setStatus(Component.literal("Connected. Model \"" + selectedModel + "\" isn't installed, pick another.").withColor(CommonColors.SOFT_YELLOW));
            }
            else {
                setStatus(Component.literal("Connected to Ollama (" + models.size() + (models.size() == 1 ? " model)" : " models)")).withColor(CommonColors.GREEN));
            }
        }, error -> {
            connectButton.active = true;
            setStatus(Component.literal("Not connected: " + error).withColor(CommonColors.SOFT_RED));
        });
    }

    private void setStatus(Component status) {
        statusText.setMessage(status);
        if (initialized) rebuildWidgets(); // the status arrives asynchronously and may change height
    }

    @Override
    protected void init() {
        initialized = true;
        int x = width / 2 - WIDGET_WIDTH / 2;
        int y = 8;

        titleText.setPosition(width / 2 - titleText.getWidth() / 2, y);
        y += 16;
        enabledButton.setPosition(x, y);
        y += 24;
        urlBox.setPosition(x, y);
        connectButton.setPosition(x + WIDGET_WIDTH - 70, y);
        y += 24;
        statusText.setPosition(width / 2 - statusText.getWidth() / 2, y);
        y += statusText.getHeight() + 6;
        modelLabel.setMaxWidth(WIDGET_WIDTH);
        modelLabel.setPosition(width / 2 - modelLabel.getWidth() / 2, y);
        int listTop = y + 12;

        // bottom-up: Done, help, adjust toggle, cut-off sliders
        int doneY = height - 26;
        doneButton.setPosition(width / 2 - doneButton.getWidth() / 2, doneY);
        int helpY = doneY - 6 - helpText.getHeight();
        helpText.setPosition(width / 2 - helpText.getWidth() / 2, helpY);
        int playY = helpY - 6 - 20;
        autoKeepPlayingButton.setPosition(x, playY);
        indicatorButton.setPosition(x + WIDGET_WIDTH / 2 + 2, playY);
        int waitY = playY - 4 - 20;
        quitAfterSlider.setPosition(x, waitY);
        minPlaySlider.setPosition(x + WIDGET_WIDTH / 2 + 2, waitY);
        int adjustY = waitY - 4 - 20;
        adjustButton.setPosition(x, adjustY);
        int slidersY = adjustY - 4 - 20;
        hardSlider.setPosition(x, slidersY);
        goodSlider.setPosition(x + SLIDER_WIDTH + 4, slidersY);
        easySlider.setPosition(x + 2 * (SLIDER_WIDTH + 4), slidersY);
        resetButton.setPosition(x + WIDGET_WIDTH - 20, slidersY);
        int cutoffLabelY = slidersY - 12;
        cutoffLabel.setPosition(width / 2 - cutoffLabel.getWidth() / 2, cutoffLabelY);

        int listHeight = Math.max(3 * 14 + 4, cutoffLabelY - 6 - listTop);
        modelList.updateSizeAndPosition(WIDGET_WIDTH, listHeight, x, listTop);

        addRenderableWidget(titleText);
        addRenderableWidget(enabledButton);
        addRenderableWidget(urlBox);
        addRenderableWidget(connectButton);
        addRenderableWidget(statusText);
        addRenderableWidget(modelLabel);
        addRenderableWidget(modelList);
        addRenderableWidget(cutoffLabel);
        addRenderableWidget(hardSlider);
        addRenderableWidget(goodSlider);
        addRenderableWidget(easySlider);
        addRenderableWidget(resetButton);
        addRenderableWidget(adjustButton);
        addRenderableWidget(quitAfterSlider);
        addRenderableWidget(minPlaySlider);
        addRenderableWidget(autoKeepPlayingButton);
        addRenderableWidget(indicatorButton);
        addRenderableWidget(helpText);
        addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        ModConfig.AiGrading newAi = new ModConfig.AiGrading(enabledButton.getValue(), url(), selectedModel,
                hardSlider.score(), goodSlider.score(), easySlider.score(), adjustButton.getValue(),
                quitAfterSlider.intValue(), indicatorButton.getValue(), minPlaySlider.intValue(), autoKeepPlayingButton.getValue());
        FileManager.updateConfig(FileManager.getConfig().withAiGrading(newAi)); // written to config/flashcards, so it survives restarts
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
