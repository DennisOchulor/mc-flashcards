package io.github.dennisochulor.flashcards;

public record ModStats(int totalQuestionsAnswered, int correctAnswers, int wrongAnswers) {

    public ModStats incrementCorrect() {
        return new ModStats(totalQuestionsAnswered + 1,correctAnswers + 1,wrongAnswers);
    }

    public ModStats incrementWrong() {
        return new ModStats(totalQuestionsAnswered + 1,correctAnswers,wrongAnswers + 1);
    }

}
