package io.github.dennisochulor.flashcards.questions;

import org.jspecify.annotations.Nullable;

public record Question(String question, @Nullable String imageName, String answer) {}