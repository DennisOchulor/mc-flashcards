package io.github.dennisochulor.flashcards.config;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.anki.AnkiSession;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
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

class AnkiConfigScreen extends Screen {
    private static final int WIDGET_WIDTH = 280;
    private static final int GRAY = 0xFFAAAAAA;

    private final Screen parent;
    private final ModConfig config = FileManager.getConfig();

    private final StringWidget titleText = new StringWidget(Component.literal("Anki Sync"), Minecraft.getInstance().font);

    private final CycleButton<Boolean> ankiModeButton = CycleButton.onOffBuilder(config.ankiMode())
            .withTooltip(_ -> Tooltip.create(Component.literal(
                    "ON: questions come from your Anki deck and your answers are saved as real Anki reviews.\n" +
                    "OFF: use the mod's own question lists.")))
            .create(0, 0, WIDGET_WIDTH, 20, Component.literal("Use Anki cards"));

    private final CycleButton<Boolean> syncButton = CycleButton.onOffBuilder(config.ankiSync())
            .withTooltip(_ -> Tooltip.create(Component.literal(
                    "Sync Anki with AnkiWeb when you join and leave a world, so reviews from your phone show up here and vice versa.\n" +
                    "You must be logged in to AnkiWeb inside Anki.")))
            .create(0, 0, WIDGET_WIDTH, 20, Component.literal("Sync with AnkiWeb on join/leave"));

    private final EditBox urlBox = new EditBox(Minecraft.getInstance().font, WIDGET_WIDTH - 75, 20, Component.literal("AnkiConnect address"));
    private final Button connectButton = Button.builder(Component.literal("Connect"), _ -> loadDecks())
            .tooltip(Tooltip.create(Component.literal("Load your decks from Anki")))
            .size(70, 20).build();

    private final MultiLineTextWidget statusText = new MultiLineTextWidget(Component.empty(), Minecraft.getInstance().font);
    private final StringWidget deckLabel = new StringWidget(Component.empty(), Minecraft.getInstance().font);
    private final EditBox searchBox = new EditBox(Minecraft.getInstance().font, WIDGET_WIDTH, 18, Component.literal("Search decks"));
    private final DeckListWidget deckList;
    private final MultiLineTextWidget helpText = new MultiLineTextWidget(Component.literal(
            "Needs the Anki desktop app open with the AnkiConnect add-on (code 2055492159). " +
            "Again counts as wrong, Hard/Good/Easy as correct.").withColor(GRAY),
            Minecraft.getInstance().font);

    private final Button doneButton = Button.builder(Component.literal("Done"), _ -> this.onClose()).size(100, 20).build();

    private String selectedDeck = config.ankiDeck();
    private boolean initialized = false;

    protected AnkiConfigScreen() {
        super(Component.literal("Anki Sync Config"));
        parent = Objects.requireNonNull(Minecraft.getInstance().gui.screen());

        urlBox.setMaxLength(200);
        urlBox.setValue(config.ankiConnectUrl());
        urlBox.setHint(Component.literal(ModConfig.DEFAULT_ANKI_CONNECT_URL));
        urlBox.setTooltip(Tooltip.create(Component.literal("AnkiConnect address. The default is " + ModConfig.DEFAULT_ANKI_CONNECT_URL)));
        statusText.setMaxWidth(WIDGET_WIDTH).setCentered(true);
        helpText.setMaxWidth(WIDGET_WIDTH + 40).setCentered(true);

        searchBox.setHint(Component.literal("Search decks...").withColor(GRAY));
        searchBox.setMaxLength(100);

        deckList = new DeckListWidget(WIDGET_WIDTH, 100, 0, deck -> {
            selectedDeck = deck;
            updateDeckLabel();
        });
        searchBox.setResponder(deckList::setFilter);
        updateDeckLabel();

        loadDecks();
    }

    private String url() {
        String url = urlBox.getValue().trim();
        return url.isEmpty() ? ModConfig.DEFAULT_ANKI_CONNECT_URL : url;
    }

    private void updateDeckLabel() {
        deckLabel.setMessage(Component.literal("Deck: ").withColor(GRAY)
                .append(selectedDeck.isBlank() ? Component.literal("(none, pick one below)").withColor(CommonColors.SOFT_YELLOW)
                        : Component.literal(selectedDeck).withColor(CommonColors.WHITE)));
        deckLabel.setX(width / 2 - deckLabel.getWidth() / 2); // re-center (no rebuild: this runs while the list handles a click)
    }

    private void loadDecks() {
        connectButton.active = false;
        setStatus(Component.literal("Connecting to Anki...").withColor(GRAY));

        AnkiSession.loadDecks(url(), loaded -> {
            connectButton.active = true;
            deckList.setDecks(loaded, selectedDeck);
            searchBox.setValue("");
            if (loaded.isEmpty()) {
                setStatus(Component.literal("Connected, but Anki has no decks.").withColor(CommonColors.SOFT_YELLOW));
                return;
            }
            if (selectedDeck.isBlank() && loaded.size() == 1) {
                selectedDeck = loaded.getFirst();
                deckList.setDecks(loaded, selectedDeck);
                updateDeckLabel();
            }
            setStatus(!selectedDeck.isBlank() && !loaded.contains(selectedDeck)
                    ? Component.literal("Connected. Deck \"" + selectedDeck + "\" wasn't found, pick another.").withColor(CommonColors.SOFT_YELLOW)
                    : Component.literal("Connected to Anki (" + loaded.size() + " decks)").withColor(CommonColors.GREEN));
        }, error -> {
            connectButton.active = true;
            setStatus(Component.literal(error).withColor(CommonColors.SOFT_RED));
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
        ankiModeButton.setPosition(x, y);
        y += 24;
        urlBox.setPosition(x, y);
        connectButton.setPosition(x + WIDGET_WIDTH - 70, y);
        y += 24;
        statusText.setPosition(width / 2 - statusText.getWidth() / 2, y);
        y += statusText.getHeight() + 6;
        deckLabel.setMaxWidth(WIDGET_WIDTH);
        deckLabel.setPosition(width / 2 - deckLabel.getWidth() / 2, y);
        y += 12;
        searchBox.setPosition(x, y);
        int listTop = y + 22;

        // bottom-up: Done, help text, sync toggle
        int doneY = height - 26;
        doneButton.setPosition(width / 2 - doneButton.getWidth() / 2, doneY);
        int helpY = doneY - 6 - helpText.getHeight();
        helpText.setPosition(width / 2 - helpText.getWidth() / 2, helpY);
        int syncY = helpY - 6 - 20;
        syncButton.setPosition(x, syncY);

        int listHeight = Math.max(3 * 14 + 4, syncY - 6 - listTop);
        deckList.updateSizeAndPosition(WIDGET_WIDTH, listHeight, x, listTop);

        addRenderableWidget(titleText);
        addRenderableWidget(ankiModeButton);
        addRenderableWidget(urlBox);
        addRenderableWidget(connectButton);
        addRenderableWidget(statusText);
        addRenderableWidget(deckLabel);
        addRenderableWidget(searchBox);
        addRenderableWidget(deckList);
        addRenderableWidget(syncButton);
        addRenderableWidget(helpText);
        addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        ModConfig newConfig = FileManager.getConfig().withAnki(ankiModeButton.getValue(), selectedDeck, url(), syncButton.getValue());
        FileManager.updateConfig(newConfig);
        QuestionScheduler.updateConfig(newConfig);
        AnkiSession.refreshStats();
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
