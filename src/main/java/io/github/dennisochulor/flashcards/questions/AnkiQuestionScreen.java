package io.github.dennisochulor.flashcards.questions;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ScoreSlider;
import io.github.dennisochulor.flashcards.ai.AiGrader;
import io.github.dennisochulor.flashcards.ai.GradingPrompt;
import io.github.dennisochulor.flashcards.ai.Ollama;
import io.github.dennisochulor.flashcards.anki.AnkiCard;
import io.github.dennisochulor.flashcards.anki.AnkiSession;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows a card from Anki's review queue in a scrollable panel at normal text size; the grade is sent back to Anki as a
 * normal review. Again counts as a wrong answer for the reward/penalty commands, the other buttons count as correct.
 * <p>
 * Classic mode, like Anki: Space/Enter shows the answer, then 1-4 (or Space/Enter = Good) grades it.
 * <p>
 * AI grading mode: the player types an answer and a local AI grades it by meaning. The score unlocks the buttons
 * (Again always; Hard/Good/Easy from the configured cut-offs). "Show Answer" means "I don't know": only Again is offered.
 * An answer the AI finds too vague (and would fail) gets one second chance, with a hint about what to be specific about.
 * <p>
 * Mouse wheel and Page Up/Down scroll long cards (arrow keys too, when not typing).
 */
public class AnkiQuestionScreen extends Screen {
    private static final int AGAIN = 1;
    private static final int MAX_CARD_WIDTH = 480;
    private static final int HEADER_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int ANSWER_BOX_HEIGHT = 56;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int EASY_BLUE = 0xFF55AAFF;

    /** A card the player left (Quit) while the AI was grading; it reopens once graded. */
    private static @Nullable AnkiQuestionScreen background = null;

    private enum Phase {
        /** Question shown; in AI mode the player is typing. */
        QUESTION,
        /** AI mode: the first answer was too vague, try once more. */
        SECOND_CHANCE,
        /** Classic mode after Show Answer: all buttons. */
        REVEALED,
        /** AI mode after Show Answer ("I don't know"): only Again. */
        GAVE_UP,
        /** Waiting for the AI. */
        GRADING,
        /** The AI couldn't grade: retry, or grade it yourself. */
        GRADE_ERROR,
        /** The AI's score decides which buttons are unlocked. */
        GRADED
    }

    private final AnkiCard card;
    private final boolean aiMode = AiGrader.isEnabled();
    private final ModConfig.AiGrading ai = FileManager.getConfig().aiGrading();
    private final CardView cardView = new CardView(Minecraft.getInstance().font);
    private final StringWidget header;
    private final MultiLineEditBox answerBox;
    private final Button showAnswerButton;
    private final Button checkButton;
    private final Button retryButton;
    private final Button gradeMyselfButton;
    private final Button quitButton;
    private final List<Button> gradeButtons = new ArrayList<>();
    private final List<Integer> gradeEases = new ArrayList<>();
    private final List<String> gradeNames = new ArrayList<>();
    private @Nullable ScoreSlider scoreSlider = null;

    private Phase phase = Phase.QUESTION;
    private boolean answered = false;
    private int answerBlockIndex = -1;
    private int attempt = 1;
    private String typedAnswer = "";
    private String firstAnswer = "";
    private String secondChanceHint = "";
    private Ollama.@Nullable Grade aiGrade = null;
    private double score = 0;
    private String gradeError = "";
    private long gradingStartedMillis = 0;
    private int thinkingY = 0;
    private boolean quitShown = false;
    private boolean scrollAfterInit = false;
    private long backgroundSinceMillis = 0;

    public AnkiQuestionScreen(AnkiCard card) {
        super(Component.literal("Anki card"));
        this.card = card;
        Font font = Minecraft.getInstance().font;

        String deck = card.deckName().isBlank() ? "Anki" : card.deckName();
        header = new StringWidget(Component.literal(deck).withColor(GRAY), font);

        answerBox = MultiLineEditBox.builder()
                .setPlaceholder(Component.literal("Type your answer...  (Enter: check, Shift+Enter: new line)").withColor(GRAY))
                .build(font, 300, ANSWER_BOX_HEIGHT, Component.literal("Your answer"));
        answerBox.setCharacterLimit(4000);
        answerBox.setValueListener(_ -> updateCheckButton());

        showAnswerButton = Button.builder(Component.literal("Show Answer"), _ -> reveal())
                .tooltip(Tooltip.create(Component.literal(aiMode ? "I don't know: see the answer, it's graded as Again" : "Space / Enter")))
                .build();
        checkButton = Button.builder(Component.literal("Check answer"), _ -> checkAnswer())
                .tooltip(Tooltip.create(Component.literal("Let the AI grade your answer (Enter)")))
                .build();
        retryButton = Button.builder(Component.literal("Retry"), _ -> checkAnswer()).build();
        gradeMyselfButton = Button.builder(Component.literal("Grade myself"), _ -> setPhase(Phase.REVEALED))
                .tooltip(Tooltip.create(Component.literal("Show the answer with all four buttons, like plain Anki")))
                .build();

        quitButton = Button.builder(Component.literal("Keep playing"), _ -> quitToGame())
                .tooltip(Tooltip.create(Component.literal("Play while the AI finishes; this card comes back when it's graded. (Space / Esc)")))
                .size(90, 20).build();

        List<Integer> buttons = card.buttons().isEmpty() ? List.of(1, 2, 3, 4) : card.buttons();
        for (int i = 0; i < buttons.size(); i++) {
            int ease = buttons.get(i);
            String name = buttonName(ease, buttons.size());
            String interval = i < card.nextReviews().size() ? card.nextReviews().get(i) : "";
            Component label = Component.literal(name).withColor(buttonColor(name))
                    .append(interval.isEmpty() ? Component.empty() : Component.literal("  " + interval).withColor(CommonColors.LIGHT_GRAY));
            Button button = Button.builder(label, _ -> grade(ease)).build();
            gradeButtons.add(button);
            gradeEases.add(ease);
            gradeNames.add(name);
        }
        updateCheckButton();
        updateGradeButtons();

        if (aiMode) AiGrader.preload(); // load the model while the player types
    }

    /** Anki shows 4 buttons in the v3 scheduler; older schedulers can offer only Again/Good(/Easy) for learning cards. */
    private static String buttonName(int ease, int buttonCount) {
        if (buttonCount == 4) return switch (ease) { case 1 -> "Again"; case 2 -> "Hard"; case 3 -> "Good"; default -> "Easy"; };
        return switch (ease) { case 1 -> "Again"; case 2 -> "Good"; default -> "Easy"; };
    }

    /** 1-4 like Anki's 4-button layout, so names map to cut-offs and definitions even on 3-button cards. */
    private static int nameToEase4(String name) {
        return switch (name) { case "Again" -> 1; case "Hard" -> 2; case "Good" -> 3; default -> 4; };
    }

    private static String ease4ToName(int ease4) {
        return switch (ease4) { case 1 -> "Again"; case 2 -> "Hard"; case 3 -> "Good"; default -> "Easy"; };
    }

    private static int buttonColor(String name) {
        return switch (name) {
            case "Again" -> CommonColors.SOFT_RED;
            case "Hard" -> CommonColors.SOFT_YELLOW;
            case "Good" -> CommonColors.GREEN;
            default -> EASY_BLUE;
        };
    }

    private double minimumScore(String name) {
        return ai.minimumScore(nameToEase4(name));
    }

    private boolean isUnlocked(int index) {
        return switch (phase) {
            case REVEALED -> true;
            case GAVE_UP -> gradeEases.get(index) == AGAIN;
            case GRADED -> score >= minimumScore(gradeNames.get(index)) - 1e-9;
            default -> false;
        };
    }

    /** The button Enter picks: Good in classic mode, the best unlocked button after AI grading. */
    private int suggestedIndex() {
        if (phase == Phase.GRADED || phase == Phase.GAVE_UP) {
            for (int i = gradeEases.size() - 1; i >= 0; i--) if (isUnlocked(i)) return i;
            return 0;
        }
        return gradeEases.indexOf(gradeEases.size() == 4 ? 3 : 2);
    }

    private boolean isTyping() {
        return aiMode && (phase == Phase.QUESTION || phase == Phase.SECOND_CHANCE);
    }

    private void updateCheckButton() {
        checkButton.active = !answerBox.getValue().isBlank();
    }

    private void updateGradeButtons() {
        for (int i = 0; i < gradeButtons.size(); i++) {
            Button button = gradeButtons.get(i);
            boolean unlocked = isUnlocked(i);
            button.active = unlocked;
            String interval = i < card.nextReviews().size() ? card.nextReviews().get(i) : "";
            String name = gradeNames.get(i);
            StringBuilder tip = new StringBuilder();
            if (!unlocked && phase == Phase.GRADED) tip.append("Locked: needs a score of ").append(format(minimumScore(name))).append("\n\n");
            tip.append(GradingPrompt.DEFINITIONS[nameToEase4(name) - 1]).append("\n\nKey: ").append(i + 1);
            if (!interval.isEmpty()) tip.append(" · next review in ").append(interval);
            button.setTooltip(Tooltip.create(Component.literal(tip.toString())));
        }
    }

    private static String format(double score) {
        return String.format(Locale.ROOT, "%.2f", score);
    }

    private int imageSize() {
        return Math.max(48, Math.min(160, height / 3));
    }

    // ---------------------------------------------------------------- card content

    private static Component heading(String text) {
        return Component.literal(text).withColor(GRAY);
    }

    private static Component userText(String text) {
        return Component.literal(text.replace('§', '$')); // a typed § would be read as a formatting code
    }

    private List<CardView.Block> questionBlocks() {
        List<CardView.Block> blocks = new ArrayList<>();
        if (card.questionImage() != null) blocks.add(new CardView.Image(card.questionImage(), imageSize()));
        if (!card.question().isBlank()) blocks.add(new CardView.Text(Component.literal(card.question()), CommonColors.WHITE));
        return blocks;
    }

    private void addCardAnswer(List<CardView.Block> blocks) {
        if (card.answerImage() != null) blocks.add(new CardView.Image(card.answerImage(), imageSize()));
        if (!card.answer().isBlank()) blocks.add(new CardView.Text(Component.literal(card.answer()), CommonColors.WHITE));
        else if (card.answerImage() == null) blocks.add(new CardView.Text(heading("(no answer text)"), GRAY));
    }

    private void addTypedAnswer(List<CardView.Block> blocks) {
        blocks.add(new CardView.Divider());
        blocks.add(new CardView.Text(heading(attempt == 2 ? "Your answer (second try)" : "Your answer"), GRAY));
        blocks.add(new CardView.Text(userText(typedAnswer), CommonColors.LIGHT_GRAY));
    }

    /** The AI's verdict: grade + score, key-point checklist, vagueness note and feedback. */
    private void addVerdict(List<CardView.Block> blocks, Ollama.Grade grade) {
        String name = ease4ToName(grade.ease());
        blocks.add(new CardView.Text(Component.literal("AI grade: ").withColor(GRAY)
                .append(Component.literal(name).withColor(buttonColor(name)))
                .append(Component.literal("  (" + format(grade.score()) + ")").withColor(GRAY)), CommonColors.WHITE));

        if (!grade.keyPoints().isEmpty()) {
            MutableComponent list = Component.empty();
            for (int i = 0; i < grade.keyPoints().size(); i++) {
                Ollama.KeyPoint point = grade.keyPoints().get(i);
                if (i > 0) list.append("\n");
                list.append(switch (point.status()) {
                    case GradingPrompt.CORRECT -> Component.literal("✔ ").withColor(CommonColors.GREEN);
                    case GradingPrompt.PARTIAL -> Component.literal("~ ").withColor(CommonColors.SOFT_YELLOW);
                    case GradingPrompt.WRONG -> Component.literal("✘ ").withColor(CommonColors.RED);
                    default -> Component.literal("✘ ").withColor(CommonColors.SOFT_RED);
                });
                list.append(Component.literal(point.point().replace('§', '$')).withColor(CommonColors.WHITE));
                if (point.status().equals(GradingPrompt.PARTIAL)) list.append(Component.literal(" (partly)").withColor(GRAY));
                if (point.status().equals(GradingPrompt.WRONG)) list.append(Component.literal(" (wrong)").withColor(CommonColors.RED));
                if (point.status().equals(GradingPrompt.MISSING)) list.append(Component.literal(" (missing)").withColor(GRAY));
            }
            blocks.add(new CardView.Text(list, CommonColors.WHITE, true));
        }
        if (grade.vague() && !grade.vagueHint().isBlank()) {
            blocks.add(new CardView.Text(Component.literal((attempt == 2 ? "Still too vague: " : "Too vague: ") + grade.vagueHint().replace('§', '$')),
                    CommonColors.SOFT_YELLOW));
        }
        if (!grade.feedback().isBlank()) blocks.add(new CardView.Text(userText(grade.feedback()), CommonColors.WHITE));
    }

    private List<CardView.Block> blocksForPhase() {
        List<CardView.Block> blocks = questionBlocks();
        switch (phase) {
            case QUESTION -> {}
            case SECOND_CHANCE -> {
                blocks.add(new CardView.Divider());
                blocks.add(new CardView.Text(heading("Your first answer"), GRAY));
                blocks.add(new CardView.Text(userText(firstAnswer), CommonColors.LIGHT_GRAY));
                blocks.add(new CardView.Text(Component.literal("Too vague. Second chance, be more specific:"), CommonColors.SOFT_YELLOW));
                blocks.add(new CardView.Text(userText(secondChanceHint), CommonColors.WHITE));
            }
            case REVEALED, GAVE_UP -> {
                blocks.add(new CardView.Divider());
                answerBlockIndex = blocks.size() - 1;
                addCardAnswer(blocks);
            }
            case GRADING -> addTypedAnswer(blocks); // the thinking animation is drawn below the card
            case GRADE_ERROR -> {
                addTypedAnswer(blocks);
                blocks.add(new CardView.Text(Component.literal("Couldn't grade: " + gradeError), CommonColors.SOFT_RED));
            }
            case GRADED -> {
                addTypedAnswer(blocks);
                answerBlockIndex = blocks.size() - 3; // the divider above "Your answer"
                blocks.add(new CardView.Divider());
                if (aiGrade != null) addVerdict(blocks, aiGrade);
                blocks.add(new CardView.Divider());
                blocks.add(new CardView.Text(heading("Card answer"), GRAY));
                addCardAnswer(blocks);
            }
        }
        return blocks;
    }

    // ---------------------------------------------------------------- layout

    @Override
    public void init() {
        // blocks depend on the screen height (image size), so rebuild them on resize
        cardView.setBlocks(blocksForPhase());

        int cardWidth = Math.min(MAX_CARD_WIDTH, width - 32);
        int cardX = (width - cardWidth) / 2;
        boolean typing = isTyping();
        boolean slider = phase == Phase.GRADED && ai.adjustScore();
        int bottomBlock = (phase == Phase.GRADING ? ThinkingAnimation.height(Minecraft.getInstance().font) + 4 : BUTTON_HEIGHT)
                + (typing ? ANSWER_BOX_HEIGHT + 6 : 0) + (slider ? BUTTON_HEIGHT + 4 : 0);
        int maxCardHeight = height - HEADER_HEIGHT - 8 - bottomBlock - 8;

        header.setMaxWidth(cardWidth);
        header.setPosition(width / 2 - header.getWidth() / 2, (HEADER_HEIGHT - 9) / 2);
        addRenderableWidget(header);

        cardView.setRectangle(cardWidth, 40, cardX, HEADER_HEIGHT); // width first, so the content can be measured
        int cardHeight = Math.max(40, Math.min(maxCardHeight, cardView.preferredHeight()));
        cardView.setHeight(cardHeight);
        cardView.refreshScrollAmount();
        addRenderableWidget(cardView);

        int y = HEADER_HEIGHT + cardHeight + 8;
        if (typing) {
            answerBox.setRectangle(cardWidth, ANSWER_BOX_HEIGHT, cardX, y);
            addRenderableWidget(answerBox);
            setInitialFocus(answerBox);
            y += ANSWER_BOX_HEIGHT + 6;
        }
        if (slider) {
            if (scoreSlider == null) {
                scoreSlider = new ScoreSlider(200, BUTTON_HEIGHT, "Your score: ", score, value -> {
                    score = value;
                    updateGradeButtons();
                });
                scoreSlider.setTooltip(Tooltip.create(Component.literal("Disagree with the AI? Drag to change the score; it unlocks the buttons.")));
            }
            scoreSlider.setPosition(width / 2 - 100, y);
            addRenderableWidget(scoreSlider);
            y += BUTTON_HEIGHT + 4;
        }
        thinkingY = y;

        switch (phase) {
            case QUESTION, SECOND_CHANCE -> {
                if (aiMode) {
                    checkButton.setMessage(Component.literal(phase == Phase.SECOND_CHANCE ? "Check again" : "Check answer"));
                    placeRow(y, cardWidth, List.of(showAnswerButton, checkButton), 120);
                    addRenderableWidget(showAnswerButton);
                    addRenderableWidget(checkButton);
                }
                else {
                    showAnswerButton.setRectangle(120, BUTTON_HEIGHT, width / 2 - 60, y);
                    addRenderableWidget(showAnswerButton);
                }
            }
            case GRADING -> { // the thinking animation goes here, see extractRenderState
                if (quitShown) {
                    quitButton.setPosition(width - quitButton.getWidth() - 6, height - quitButton.getHeight() - 6);
                    addRenderableWidget(quitButton);
                }
            }
            case GRADE_ERROR -> {
                placeRow(y, cardWidth, List.of(retryButton, gradeMyselfButton), 120);
                addRenderableWidget(retryButton);
                addRenderableWidget(gradeMyselfButton);
            }
            case GAVE_UP -> {
                int again = gradeEases.indexOf(AGAIN);
                Button button = gradeButtons.get(Math.max(0, again));
                button.setRectangle(120, BUTTON_HEIGHT, width / 2 - 60, y);
                addRenderableWidget(button);
                setInitialFocus(button);
            }
            case REVEALED, GRADED -> {
                placeRow(y, cardWidth, gradeButtons, 110);
                gradeButtons.forEach(this::addRenderableWidget);
            }
        }

        if (scrollAfterInit) { // reopened after grading in the background
            scrollAfterInit = false;
            scrollForPhase();
        }
    }

    private void placeRow(int y, int availableWidth, List<Button> buttons, int maxWidth) {
        int count = buttons.size();
        int buttonWidth = Math.min(maxWidth, (availableWidth - BUTTON_GAP * (count - 1)) / Math.max(1, count));
        int totalWidth = count * buttonWidth + (count - 1) * BUTTON_GAP;
        for (int i = 0; i < count; i++) {
            buttons.get(i).setRectangle(buttonWidth, BUTTON_HEIGHT, width / 2 - totalWidth / 2 + i * (buttonWidth + BUTTON_GAP), y);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (phase == Phase.GRADING) extractThinking(graphics);
    }

    private void extractThinking(GuiGraphicsExtractor graphics) {
        Font font = Minecraft.getInstance().font;
        int animWidth = ThinkingAnimation.width(font, ai.model());
        ThinkingAnimation.draw(graphics, font, width / 2 - animWidth / 2, thinkingY, gradingElapsedMillis(), ai.model());
    }

    public long gradingElapsedMillis() {
        return System.currentTimeMillis() - gradingStartedMillis;
    }

    @Override
    public void tick() {
        super.tick();
        // Thinking too long: offer a way back to the game
        if (phase == Phase.GRADING && !quitShown && gradingElapsedMillis() >= ai.quitAfterSeconds() * 1000L) {
            quitShown = true;
            rebuildWidgets();
        }
    }

    // ---------------------------------------------------------------- background grading

    /** Leaves the card while the AI keeps grading; {@link #tickBackground} brings it back when done. */
    private void quitToGame() {
        if (phase != Phase.GRADING) return;
        background = this;
        backgroundSinceMillis = System.currentTimeMillis();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(null);
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.literal("Keep playing, the card comes back when the AI has graded it").withColor(GRAY));
        }
    }

    /** @return the card being graded while the player plays, or null */
    public static @Nullable AnkiQuestionScreen backgroundCard() {
        return background;
    }

    public boolean isGrading() {
        return phase == Phase.GRADING;
    }

    public String modelName() {
        return ai.model();
    }

    /** @return seconds until the card may come back (the configured minimum play time), 0 if it may come back now */
    public int secondsUntilReturn() {
        long left = ai.minPlaySeconds() * 1000L - (System.currentTimeMillis() - backgroundSinceMillis);
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
    }

    /** Call every client tick: reopens a card graded in the background, once the player isn't busy (same rules as new questions). */
    public static void tickBackground(Minecraft minecraft) {
        AnkiQuestionScreen screen = background;
        if (screen == null || screen.phase == Phase.GRADING || screen.secondsUntilReturn() > 0) return;
        if (minecraft.player == null || minecraft.gui.screen() != null || QuestionScheduler.wasPlayerHurtRecently()) return;
        background = null;
        screen.scrollAfterInit = true;
        minecraft.gui.setScreen(screen);
    }

    /** Call when leaving the world: the unanswered card simply stays due in Anki. */
    public static void clearBackground() {
        background = null;
    }

    // ---------------------------------------------------------------- actions

    private void setPhase(Phase newPhase) {
        phase = newPhase;
        updateGradeButtons();
        if (background == this) return; // not on screen: tickBackground reopens it, and init lays it out then
        setFocused(null);
        rebuildWidgets();
        scrollForPhase();
    }

    private void scrollForPhase() {
        if (phase == Phase.REVEALED || phase == Phase.GAVE_UP || phase == Phase.GRADED) jumpToAnswer();
        if (phase == Phase.SECOND_CHANCE || phase == Phase.GRADING) cardView.setScrollAmount(cardView.maxScrollAmount());
    }

    /** Long card: jump to the answer if it starts below the middle of the panel (scrolling up shows the rest again). */
    private void jumpToAnswer() {
        if (cardView.maxScrollAmount() > 0 && cardView.blockTop(answerBlockIndex) > cardView.getHeight() / 2) {
            cardView.scrollToBlock(answerBlockIndex);
        }
    }

    private void reveal() {
        if (phase != Phase.QUESTION && phase != Phase.SECOND_CHANCE) return;
        setPhase(aiMode ? Phase.GAVE_UP : Phase.REVEALED);
    }

    private void checkAnswer() {
        if (isTyping()) typedAnswer = answerBox.getValue().strip();
        else if (phase != Phase.GRADE_ERROR) return;
        if (typedAnswer.isEmpty()) return;

        gradingStartedMillis = System.currentTimeMillis();
        quitShown = false;
        setPhase(Phase.GRADING);
        if (ai.autoKeepPlaying()) quitToGame(); // play while the AI thinks, without asking
        AiGrader.grade(plain(card.question()), plain(card.answer()), typedAnswer, grade -> {
            if (phase != Phase.GRADING) return;
            if (attempt == 1 && grade.ease() == AGAIN && isVagueButOnTrack(grade)) {
                // One second chance: say what to be specific about, keep the answer so it can be extended
                attempt = 2;
                firstAnswer = typedAnswer;
                secondChanceHint = grade.vagueHint().isBlank() ? "Be more specific about the key ideas the question asks for." : grade.vagueHint();
                setPhase(Phase.SECOND_CHANCE);
                return;
            }
            aiGrade = grade;
            score = grade.score();
            setPhase(Phase.GRADED);
        }, error -> {
            if (phase != Phase.GRADING) return;
            gradeError = error;
            setPhase(Phase.GRADE_ERROR);
        });
    }

    /**
     * A second chance is only for answers that are on the right track but too unspecific: the AI must call it vague,
     * and the key points must show some truth (something correct/partly correct) and nothing wrong.
     * "I don't know", excuses, off-topic and wrong answers fail straight away.
     */
    private static boolean isVagueButOnTrack(Ollama.Grade grade) {
        if (!grade.vague()) return false;
        boolean somethingRight = false;
        for (Ollama.KeyPoint point : grade.keyPoints()) {
            if (point.status().equals(GradingPrompt.WRONG)) return false;
            if (point.status().equals(GradingPrompt.CORRECT) || point.status().equals(GradingPrompt.PARTIAL)) somethingRight = true;
        }
        return somethingRight;
    }

    /** Card text without Minecraft formatting codes, for the AI. */
    private static String plain(String text) {
        return text.replaceAll("§.", "");
    }

    private void grade(int ease) {
        int index = gradeEases.indexOf(ease);
        if (answered || index < 0 || !isUnlocked(index)) return;
        answered = true;
        boolean correct = ease != AGAIN;

        AnkiSession.answer(card, ease);
        AnswerOutcome.playSoundAndRecordStats(correct);
        QuestionScheduler.schedule();
        this.onClose();
        AnswerOutcome.runCommands(correct);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        cardView.scrollBy(-scrollY * Minecraft.getInstance().font.lineHeight * 2); // scroll the card from anywhere on screen
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean typing = isTyping();
        boolean confirm = key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER
                || (!typing && key == InputConstants.KEY_SPACE);

        if (phase == Phase.GRADING && quitShown && (key == InputConstants.KEY_SPACE || key == InputConstants.KEY_ESCAPE)) {
            quitToGame();
            return true;
        }

        if (typing) {
            if (confirm && !event.hasShiftDown()) {
                checkAnswer(); // Shift+Enter falls through to the text box as a new line
                return true;
            }
            switch (key) {
                case InputConstants.KEY_PAGEUP -> { cardView.scrollBy(-cardView.pageHeight()); return true; }
                case InputConstants.KEY_PAGEDOWN -> { cardView.scrollBy(cardView.pageHeight()); return true; }
                default -> { return super.keyPressed(event); } // everything else is typing
            }
        }

        boolean sliderFocused = scoreSlider != null && getFocused() == scoreSlider;
        int line = Minecraft.getInstance().font.lineHeight;
        switch (key) {
            case InputConstants.KEY_UP -> { cardView.scrollBy(-line * 2); return true; }
            case InputConstants.KEY_DOWN -> { cardView.scrollBy(line * 2); return true; }
            case InputConstants.KEY_PAGEUP -> { cardView.scrollBy(-cardView.pageHeight()); return true; }
            case InputConstants.KEY_PAGEDOWN -> { cardView.scrollBy(cardView.pageHeight()); return true; }
            case InputConstants.KEY_HOME -> { cardView.setScrollAmount(0); return true; }
            case InputConstants.KEY_END -> { cardView.setScrollAmount(cardView.maxScrollAmount()); return true; }
            default -> {}
        }

        switch (phase) {
            case QUESTION, SECOND_CHANCE -> {
                if (confirm) {
                    reveal();
                    return true;
                }
            }
            case GRADE_ERROR -> {
                if (confirm) {
                    checkAnswer();
                    return true;
                }
            }
            case REVEALED, GAVE_UP, GRADED -> {
                int index = switch (key) {
                    case InputConstants.KEY_1, InputConstants.KEY_NUMPAD1 -> 0;
                    case InputConstants.KEY_2, InputConstants.KEY_NUMPAD2 -> 1;
                    case InputConstants.KEY_3, InputConstants.KEY_NUMPAD3 -> 2;
                    case InputConstants.KEY_4, InputConstants.KEY_NUMPAD4 -> 3;
                    default -> -1;
                };
                if (confirm && !sliderFocused) index = suggestedIndex();
                if (index >= 0 && index < gradeEases.size()) {
                    grade(gradeEases.get(index)); // ignored if that button is locked
                    return true;
                }
            }
            case GRADING -> {}
        }
        return super.keyPressed(event);
    }
}
