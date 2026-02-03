package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.ImageUtils;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Environment(EnvType.CLIENT)
class AutoValidationResultScreen extends Screen {

    private final StringWidget titleText = new StringWidget(Component.literal("Your answer is "), Minecraft.getInstance().font);
    private final StringWidget resultText;
    private final Button doneButton;
    private final MultiLineTextWidget questionText;
    private final MultiLineTextWidget yourAnswerText;
    private final MultiLineTextWidget correctAnswerText;
    @Nullable
    private final ImageWidget imageWidget;
    private final boolean isCorrect;

    protected AutoValidationResultScreen(Question question, String userAnswer) {
        super(Component.literal("Question Result"));
        isCorrect = question.answer().equalsIgnoreCase(userAnswer);

        resultText = new StringWidget(Component.literal(isCorrect ? "CORRECT" : "WRONG").withColor(isCorrect ? CommonColors.GREEN : CommonColors.SOFT_RED), Minecraft.getInstance().font);

        questionText = new ScalableMultilineTextWidget(Component.literal("§n§lQuestion:§r\n" + question.question()), Minecraft.getInstance().font, 110);

        yourAnswerText = new ScalableMultilineTextWidget(Component.literal("§n§lYour answer:§r\n" + userAnswer), Minecraft.getInstance().font, 75);

        correctAnswerText  = new ScalableMultilineTextWidget(Component.literal("§n§lCorrect answer:§r\n" + question.answer()), Minecraft.getInstance().font, 75);

        Runnable releaseImgResource;
        if (question.imageName() != null) {
            ImageUtils.ImagePackage imgPkg = ImageUtils.getImagePackage(FileManager.getImageFile(question.imageName()));
            if (imgPkg == null) {
                releaseImgResource = () -> {};
                imageWidget = ImageWidget.texture(140,140,Identifier.withDefaultNamespace("textures/missing.png"),140,140);
                imageWidget.setTooltip(Tooltip.create(Component.literal(question.imageName() + " seems to be missing...")));
            }
            else {
                releaseImgResource = () -> Minecraft.getInstance().getTextureManager().release(imgPkg.id());
                int width = (int)(140 * imgPkg.widthScaler());
                int height = (int)(140 * imgPkg.heightScaler());
                imageWidget = ImageWidget.texture(width,height,imgPkg.id(),width,height);
            }
        }
        else {
            imageWidget = null;
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
        }).build();

        Objects.requireNonNull(Minecraft.getInstance().player).playSound(isCorrect ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ANVIL_LAND);
        ModStats stats = FileManager.getStats();
        if (isCorrect) FileManager.updateStats(stats.incrementCorrect());
        else FileManager.updateStats(stats.incrementWrong());
    }

    @Override
    public void init() {
        if (isCorrect) titleText.setPosition(width/2 - 57, 15);
        else titleText.setPosition(width/2 - 51, 15);
        resultText.setPosition(titleText.getX() + 80, titleText.getY());

        questionText.setWidth(250);
        questionText.setMaxWidth(250);
        questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2, 45);
        questionText.setCentered(true);

        yourAnswerText.setWidth(175);
        yourAnswerText.setMaxWidth(175);
        yourAnswerText.setPosition(width/4 - Math.min(yourAnswerText.getWidth(), 150)/2, 170);
        yourAnswerText.setCentered(true);

        correctAnswerText.setWidth(175);
        correctAnswerText.setMaxWidth(175);
        correctAnswerText.setPosition(width/2 + width/4 - Math.min(correctAnswerText.getWidth(), 150)/2, 170);
        correctAnswerText.setCentered(true);

        doneButton.setRectangle(75,20,width/2 - 37,height - 30);

        addRenderableOnly(titleText);
        addRenderableOnly(resultText);
        addRenderableOnly(questionText);
        addRenderableOnly(yourAnswerText);
        addRenderableOnly(correctAnswerText);
        addRenderableWidget(doneButton);
        if (imageWidget != null) {
            addRenderableOnly(imageWidget);
            questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2 + 100, 45);
            imageWidget.setPosition(width/4 - 75,25);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
