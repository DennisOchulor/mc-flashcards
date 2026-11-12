package io.github.dennisochulor.flashcards.anki;

import org.jspecify.annotations.Nullable;

/** Numbers shown on the HUD: answers in this play session, plus Anki's own counts when Anki mode is on. */
public final class StudyStats {
    private StudyStats() {}

    /**
     * @param reviewedToday cards reviewed today across the whole Anki collection (including on other devices)
     * @param newCount      new cards still to study today in the deck, -1 if unknown (same for the other counts)
     */
    public record AnkiStats(int reviewedToday, int newCount, int learnCount, int reviewCount) {
        public boolean hasDeckCounts() {
            return newCount >= 0;
        }

        public int dueTotal() {
            return Math.max(0, newCount) + Math.max(0, learnCount) + Math.max(0, reviewCount);
        }
    }

    private static int sessionCorrect = 0;
    private static int sessionWrong = 0;
    private static volatile @Nullable AnkiStats anki = null; // written on the Anki thread

    /** Call when joining a world. */
    public static void resetSession() {
        sessionCorrect = 0;
        sessionWrong = 0;
    }

    public static void recordAnswer(boolean correct) {
        if (correct) sessionCorrect++;
        else sessionWrong++;
    }

    public static int sessionCorrect() {
        return sessionCorrect;
    }

    public static int sessionWrong() {
        return sessionWrong;
    }

    public static int sessionTotal() {
        return sessionCorrect + sessionWrong;
    }

    public static @Nullable AnkiStats anki() {
        return anki;
    }

    static void setAnki(@Nullable AnkiStats stats) {
        anki = stats;
    }
}
