package io.github.dennisochulor.flashcards.anki;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.Flashcards;
import io.github.dennisochulor.flashcards.ImageUtils;
import io.github.dennisochulor.flashcards.config.ModConfig;
import io.github.dennisochulor.flashcards.questions.AnkiQuestionScreen;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Talks to Anki (through AnkiConnect) on a background thread and hands results back to the client thread.
 * <p>
 * Cards come from Anki's own reviewer queue for the configured deck, so daily limits, learning steps and
 * new-card order behave exactly as when reviewing inside Anki, and every answer is recorded as a normal review.
 */
public final class AnkiSession {
    private AnkiSession() {}

    /** Single thread so requests (sync, fetch, answer) reach Anki in the order they were made. Daemon so quitting never hangs. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Flashcards-AnkiConnect");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);
    private static final String MEDIA_SUBFOLDER = "anki";

    public static boolean isEnabled() {
        return FileManager.getConfig().ankiMode();
    }

    private static AnkiConnect client() {
        return new AnkiConnect(FileManager.getConfig().ankiConnectUrl());
    }

    // ---------------------------------------------------------------- prompting

    /**
     * Fetches Anki's current card for the configured deck and opens it in-game. Call on the client thread.
     * @return false if no deck is configured (nothing will be shown)
     */
    public static boolean promptCard() {
        ModConfig config = FileManager.getConfig();
        if (config.ankiDeck().isBlank()) {
            notifyPlayer(Component.literal("Flashcards: pick an Anki deck first (Flashcards config → Anki Sync...).").withColor(CommonColors.SOFT_RED), true);
            return false;
        }
        if (!FETCHING.compareAndSet(false, true)) return true; // a card is already on its way

        String deck = config.ankiDeck();
        EXECUTOR.execute(() -> {
            try {
                AnkiCard card = fetchCard(deck);
                updateStats(deck);
                Minecraft.getInstance().execute(() -> {
                    if (card == null) {
                        notifyPlayer(Component.literal("Anki: nothing left to study in \"" + deck + "\" today. Nice work!").withColor(CommonColors.GREEN), false);
                        QuestionScheduler.schedule();
                        FETCHING.set(false);
                    }
                    else {
                        openWhenFree(card);
                    }
                });
            }
            catch (AnkiConnect.AnkiException e) {
                Flashcards.LOGGER.warn("Failed to fetch Anki card", e);
                Minecraft.getInstance().execute(() -> {
                    notifyPlayer(Component.literal(e.getMessage()).withColor(CommonColors.SOFT_RED), true);
                    QuestionScheduler.schedule(); // try again next interval
                    FETCHING.set(false);
                });
            }
            catch (RuntimeException e) {
                Flashcards.LOGGER.error("Unexpected error while fetching Anki card", e);
                Minecraft.getInstance().execute(() -> {
                    notifyPlayer(Component.literal("Flashcards: unexpected error talking to Anki, see the log.").withColor(CommonColors.SOFT_RED), true);
                    QuestionScheduler.schedule();
                    FETCHING.set(false);
                });
            }
        });
        return true;
    }

    /** Opens the card, waiting while another screen is open or the player was just hurt (same rules as normal questions). */
    private static void openWhenFree(AnkiCard card) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) { // left the world meanwhile
            FETCHING.set(false);
            return;
        }
        if (minecraft.gui.screen() != null || QuestionScheduler.wasPlayerHurtRecently()) {
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> minecraft.execute(() -> openWhenFree(card)));
            return;
        }
        FETCHING.set(false);
        minecraft.gui.setScreen(new AnkiQuestionScreen(card));
    }

    private static @Nullable AnkiCard fetchCard(String deck) throws AnkiConnect.AnkiException {
        AnkiConnect anki = client();
        if (!anki.guiDeckReview(deck)) {
            if (!anki.deckNames().contains(deck)) {
                throw new AnkiConnect.AnkiException("Anki: there's no deck called \"" + deck + "\". Pick one in Flashcards config → Anki Sync...");
            }
            return null; // deck exists, nothing due
        }
        AnkiConnect.CurrentCard current = anki.guiCurrentCard();
        if (current == null) return null;

        AnkiHtml.Rendered question = AnkiHtml.render(current.questionHtml());
        AnkiHtml.Rendered answer = AnkiHtml.render(AnkiHtml.answerOnly(current.answerHtml()));
        String questionImage = downloadFirstImage(anki, question.images());
        List<String> answerOnlyImages = answer.images().stream().filter(i -> !question.images().contains(i)).toList();
        String answerImage = downloadFirstImage(anki, answerOnlyImages);

        String questionText = question.text().isBlank() && questionImage == null ? "(empty card front)" : question.text();
        return new AnkiCard(current.cardId(), current.deckName(), questionText, questionImage, answer.text(), answerImage,
                current.buttons(), current.nextReviews());
    }

    /** Copies the first displayable image from Anki's media folder into the mod's media folder. */
    private static @Nullable String downloadFirstImage(AnkiConnect anki, List<String> filenames) {
        for (String filename : filenames) {
            String safeName = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
            String relative = MEDIA_SUBFOLDER + "/" + safeName;
            File target = FileManager.getImageFile(relative);
            if (!ImageUtils.FILE_NAME_EXTENSION_FILTER.accept(target)) continue; // e.g. svg/webp, which we can't draw
            if (target.exists() && target.length() > 0) return relative;
            try {
                byte[] data = anki.retrieveMediaFile(filename);
                if (data == null) {
                    String decoded = URLDecoder.decode(filename, StandardCharsets.UTF_8);
                    if (!decoded.equals(filename)) data = anki.retrieveMediaFile(decoded);
                }
                if (data == null) continue;
                Files.createDirectories(target.getParentFile().toPath());
                Files.write(target.toPath(), data);
                return relative;
            }
            catch (AnkiConnect.AnkiException | IOException e) {
                Flashcards.LOGGER.warn("Couldn't copy Anki image {}", filename, e);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- answering

    /**
     * Sends the review to Anki. Only answers if Anki's reviewer is still on the same card, so reviewing
     * in Anki meanwhile can't cause the wrong card to be graded. Call on the client thread.
     */
    public static void answer(AnkiCard card, int ease) {
        EXECUTOR.execute(() -> {
            try {
                AnkiConnect anki = client();
                AnkiConnect.CurrentCard current = anki.guiCurrentCard();
                if (current == null || current.cardId() != card.cardId()) {
                    // Anki moved on (e.g. the player reviewed in Anki). Re-open the deck and look again before giving up.
                    anki.guiDeckReview(card.deckName().isBlank() ? FileManager.getConfig().ankiDeck() : card.deckName());
                    current = anki.guiCurrentCard();
                }
                if (current == null || current.cardId() != card.cardId()) {
                    throw new AnkiConnect.AnkiException("Anki moved on to a different card, so this answer wasn't recorded.");
                }
                anki.guiShowAnswer();
                if (!anki.guiAnswerCard(ease)) {
                    throw new AnkiConnect.AnkiException("Anki didn't accept the answer for this card.");
                }
            }
            catch (AnkiConnect.AnkiException e) {
                Flashcards.LOGGER.warn("Failed to send Anki answer", e);
                Minecraft.getInstance().execute(() -> notifyPlayer(Component.literal(e.getMessage()).withColor(CommonColors.SOFT_RED), true));
            }
            catch (RuntimeException e) {
                Flashcards.LOGGER.error("Unexpected error while sending Anki answer", e);
            }
            updateStats(FileManager.getConfig().ankiDeck());
        });
    }

    // ---------------------------------------------------------------- stats for the HUD

    /** Refreshes the Anki numbers shown on the HUD (cards reviewed today, cards still due in the deck). */
    public static void refreshStats() {
        if (!isEnabled()) {
            StudyStats.setAnki(null);
            return;
        }
        String deck = FileManager.getConfig().ankiDeck();
        EXECUTOR.execute(() -> updateStats(deck));
    }

    /** Runs on the Anki thread. Failures just clear the numbers; the player already gets told about connection problems elsewhere. */
    private static void updateStats(String deck) {
        try {
            AnkiConnect anki = client();
            int reviewedToday = anki.numCardsReviewedToday();
            AnkiConnect.DeckCounts counts = deck.isBlank() ? null : anki.deckCounts(deck);
            StudyStats.setAnki(new StudyStats.AnkiStats(reviewedToday,
                    counts == null ? -1 : counts.newCount(), counts == null ? -1 : counts.learnCount(), counts == null ? -1 : counts.reviewCount()));
        }
        catch (AnkiConnect.AnkiException | RuntimeException e) {
            Flashcards.LOGGER.debug("Couldn't refresh Anki stats", e);
            StudyStats.setAnki(null);
        }
    }

    // ---------------------------------------------------------------- syncing

    /** Asks Anki to sync with AnkiWeb, if enabled in the config. */
    public static void syncIfEnabled(boolean announce) {
        ModConfig config = FileManager.getConfig();
        if (!config.ankiMode() || !config.ankiSync()) return;
        EXECUTOR.execute(() -> {
            try {
                client().sync();
                Flashcards.LOGGER.info("Synced Anki with AnkiWeb");
                if (announce) Minecraft.getInstance().execute(() ->
                        notifyPlayer(Component.literal("Anki synced with AnkiWeb").withColor(CommonColors.GREEN), false));
            }
            catch (AnkiConnect.AnkiException e) {
                Flashcards.LOGGER.warn("AnkiWeb sync failed", e);
                if (announce) Minecraft.getInstance().execute(() ->
                        notifyPlayer(Component.literal("AnkiWeb sync failed: " + e.getMessage()).withColor(CommonColors.SOFT_RED), true));
            }
            catch (RuntimeException e) {
                Flashcards.LOGGER.error("Unexpected error during AnkiWeb sync", e);
            }
        });
    }

    // ---------------------------------------------------------------- config screen helpers

    /** Loads deck names in the background; exactly one of the callbacks runs, on the client thread. */
    public static void loadDecks(String url, Consumer<List<String>> onSuccess, Consumer<String> onError) {
        EXECUTOR.execute(() -> {
            try {
                AnkiConnect anki = new AnkiConnect(url);
                List<String> decks = anki.deckNames();
                Minecraft.getInstance().execute(() -> onSuccess.accept(decks));
            }
            catch (AnkiConnect.AnkiException e) {
                Minecraft.getInstance().execute(() -> onError.accept(e.getMessage()));
            }
            catch (RuntimeException e) { // e.g. a malformed URL
                Minecraft.getInstance().execute(() -> onError.accept("Invalid AnkiConnect address: " + e.getMessage()));
            }
        });
    }

    private static void notifyPlayer(Component message, boolean chat) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (chat) player.sendSystemMessage(message);
        else player.sendOverlayMessage(message);
    }
}
