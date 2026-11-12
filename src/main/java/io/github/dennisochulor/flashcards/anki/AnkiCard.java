package io.github.dennisochulor.flashcards.anki;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A card from Anki's review queue, ready to show in-game.
 * Image names are relative to the mod's media folder (see {@link io.github.dennisochulor.flashcards.FileManager#getImageFile}).
 *
 * @param buttons     the ease values Anki offers for this card (1 = Again ... 4 = Easy)
 * @param nextReviews Anki's interval preview for each button, e.g. "&lt;10m", "4d" (same order as {@code buttons}, may be empty)
 */
public record AnkiCard(long cardId, String deckName,
                       String question, @Nullable String questionImage,
                       String answer, @Nullable String answerImage,
                       List<Integer> buttons, List<String> nextReviews) {}
