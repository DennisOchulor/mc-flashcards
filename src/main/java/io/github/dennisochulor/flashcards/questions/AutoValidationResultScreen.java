package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.ImageUtils;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;

import java.util.Objects;

class AutoValidationResultScreen extends Screen {

    private static final int QUESTION_TEXT_WIDTH = 250;
    private static final int ANSWER_TEXT_WIDTH = 175;

    private final StringWidget titleText = new StringWidget(Component.literal("Your answer is "), Minecraft.getInstance().font);
    private final StringWidget resultText;
    private final Button doneButton;
    private final ScalableMultilineTextWidget questionText;
    private final ScalableMultilineTextWidget yourAnswerText;
    private final ScalableMultilineTextWidget correctAnswerText;
    private final boolean isCorrect;
    private final Question question;

    protected AutoValidationResultScreen(Question question, String userAnswer) {
        super(Component.literal("Question Result"));
        isCorrect = question.answer().equalsIgnoreCase(userAnswer);
        this.question = question;

        resultText = new StringWidget(Component.literal(isCorrect ? "CORRECT" : "WRONG").withColor(isCorrect ? CommonColors.GREEN : CommonColors.SOFT_RED), Minecraft.getInstance().font);

        questionText = new ScalableMultilineTextWidget(Component.literal("§n§lQuestion:§r\n" + question.question()), Minecraft.getInstance().font, 110);
        questionText.setMaxWidth(QUESTION_TEXT_WIDTH).setCentered(true);

        yourAnswerText = new ScalableMultilineTextWidget(Component.literal("§n§lYour answer:§r\n" + userAnswer), Minecraft.getInstance().font, 75);
        yourAnswerText.setMaxWidth(ANSWER_TEXT_WIDTH).setCentered(true);

        correctAnswerText = new ScalableMultilineTextWidget(Component.literal("§n§lCorrect answer:§r\n" + question.answer()), Minecraft.getInstance().font, 75);
        correctAnswerText.setMaxWidth(ANSWER_TEXT_WIDTH).setCentered(true);

        Runnable releaseImgResource;
        if (question.imageName() != null) {
            ImageUtils.ImagePackage imgPkg = ImageUtils.getImagePackage(FileManager.getImageFile(question.imageName()));
            releaseImgResource = imgPkg != null ? () -> Minecraft.getInstance().getTextureManager().release(imgPkg.id()) : () -> {};
        }
        else {
            releaseImgResource = () -> {};
        }

        doneButton = Button.builder(Component.literal("Done"), _ -> {
            QuestionScheduler.schedule();
            this.onClose();
            releaseImgResource.run();
            ModConfig config = FileManager.getConfig();

            LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
            ClientPacketListener packetListener = Objects.requireNonNull(Minecraft.getInstance().getConnection());

            // run correct/wrong commands
            switch(config.commandSelectionStrategy()) {
                case EXECUTE_ALL -> {
                    if (isCorrect) config.correctAnswerCommands().forEach(c -> packetListener.sendCommand("execute as @s at @s run " + c));
                    else config.wrongAnswerCommands().forEach(c -> packetListener.sendCommand("execute as @s at @s run " + c));
                }
                case RANDOMISE_ONE -> {
                    RandomSource random = player.getRandom();
                    String command = isCorrect ? config.correctAnswerCommands().get(random.nextInt(config.correctAnswerCommands().size())) : config.wrongAnswerCommands().get(random.nextInt(config.wrongAnswerCommands().size()));
                    Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + command);
                }
                case OFF -> {}
            }
        }).size(75, 20).build();

        Objects.requireNonNull(Minecraft.getInstance().player).playSound(isCorrect ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ANVIL_LAND);
        ModStats stats = FileManager.getStats();
        if (isCorrect) FileManager.updateStats(stats.incrementCorrect());
        else FileManager.updateStats(stats.incrementWrong());
    }

    @Override
    public void init() {
        int partHeight = (int) (0.3 * this.height);

        LinearLayout titleLayout = LinearLayout.horizontal();
        titleLayout.defaultCellSetting().alignHorizontallyCenter();
        titleLayout.addChild(titleText);
        titleLayout.addChild(resultText);

        LinearLayout questionLayout = LinearLayout.horizontal().spacing(20);
        questionLayout.defaultCellSetting().alignHorizontallyCenter();
        if (question.imageName() != null) {
            ImageWidget imageWidget = ImageUtils.getImageWidget(question.imageName(), partHeight);
            questionLayout.addChild(imageWidget);
        }
        questionText.setMaxHeigth(partHeight);
        questionLayout.addChild(questionText);

        LinearLayout answerLayout = LinearLayout.horizontal().spacing(50);
        answerLayout.defaultCellSetting().alignHorizontallyCenter();
        yourAnswerText.setMaxHeigth(partHeight);
        answerLayout.addChild(yourAnswerText, answerLayout.newCellSettings().paddingHorizontal((ANSWER_TEXT_WIDTH - yourAnswerText.getWidth()) / 2));
        correctAnswerText.setMaxHeigth(partHeight);
        answerLayout.addChild(correctAnswerText, answerLayout.newCellSettings().paddingHorizontal((ANSWER_TEXT_WIDTH - correctAnswerText.getWidth()) / 2));

        LinearLayout contentLayout = LinearLayout.vertical().spacing(25);
        contentLayout.defaultCellSetting().alignHorizontallyCenter();
        contentLayout.addChild(questionLayout);
        contentLayout.addChild(answerLayout);

        HeaderAndFooterLayout root = new HeaderAndFooterLayout(this, 20, 40);
        root.addToHeader(titleLayout);
        root.addToContents(contentLayout);
        root.addToFooter(doneButton);

        root.arrangeElements();
        FrameLayout.alignInRectangle(root, 0, 0, this.width, this.height, 0.5F, 0.1F);
        root.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
