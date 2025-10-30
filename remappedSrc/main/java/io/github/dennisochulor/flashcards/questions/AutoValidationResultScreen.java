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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
class AutoValidationResultScreen extends Screen {

    private final StringWidget titleText = new StringWidget(Component.literal("Your answer is "), Minecraft.getInstance().font);
    private final StringWidget resultText;
    private final Button doneButton;
    private final MultiLineTextWidget questionText;
    private final MultiLineTextWidget yourAnswerText;
    private final MultiLineTextWidget correctAnswerText;
    private final ImageWidget imageWidget;
    private final boolean isCorrect;

    protected AutoValidationResultScreen(Question question, String userAnswer) {
        super(Component.literal("Question Result"));
        isCorrect = question.answer().equalsIgnoreCase(userAnswer);
        ImageUtils.ImagePackage imgPkg = ImageUtils.getImageId(FileManager.getImage(question.imageName()));

        resultText = new StringWidget(Component.literal(isCorrect ? "CORRECT" : "WRONG"), Minecraft.getInstance().font);
        if(isCorrect) resultText.setColor(CommonColors.GREEN);
        else resultText.setColor(CommonColors.SOFT_RED);

        questionText = new ScalableMultilineTextWidget(Component.literal("§n§lQuestion:§r\n" + question.question()), Minecraft.getInstance().font, 110);

        yourAnswerText = new ScalableMultilineTextWidget(Component.literal("§n§lYour answer:§r\n" + userAnswer), Minecraft.getInstance().font, 75);

        correctAnswerText  = new ScalableMultilineTextWidget(Component.literal("§n§lCorrect answer:§r\n" + question.answer()), Minecraft.getInstance().font, 75);

        doneButton = Button.builder(Component.literal("Done"), button -> {
            QuestionScheduler.schedule();
            this.onClose();
            if(imgPkg != null) Minecraft.getInstance().getTextureManager().release(imgPkg.id());
            ModConfig config = FileManager.getConfig();

            // run correct/wrong commands if applicable
            if(Minecraft.getInstance().hasSingleplayerServer() || Minecraft.getInstance().player.hasPermissions(2)) {
                switch(config.commandSelectionStrategy()) {
                    case EXECUTE_ALL -> {
                        if(isCorrect) config.correctAnswerCommands().forEach(c -> Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + c));
                        else config.wrongAnswerCommands().forEach(c -> Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + c));
                    }
                    case RANDOMISE_ONE -> {
                        RandomSource random = Minecraft.getInstance().player.getRandom();
                        String command = isCorrect ? config.correctAnswerCommands().get(random.nextInt(config.correctAnswerCommands().size())) : config.wrongAnswerCommands().get(random.nextInt(config.wrongAnswerCommands().size()));
                        Minecraft.getInstance().getConnection().sendCommand("execute as @s at @s run " + command);
                    }
                }
            }
        }).build();

        if(question.imageName() != null) {
            if(imgPkg == null) {
                imageWidget = ImageWidget.texture(140,140,ResourceLocation.withDefaultNamespace("textures/missing.png"),140,140);
                imageWidget.setTooltip(Tooltip.create(Component.literal(question.imageName() + " seems to be missing...")));
            }
            else {
                int width = (int)(140 * imgPkg.widthScaler());
                int height = (int)(140 * imgPkg.heightScaler());
                imageWidget = ImageWidget.texture(width,height,imgPkg.id(),width,height);
            }
        }
        else imageWidget = null;

        if(isCorrect) Minecraft.getInstance().player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER,1,1);
        else Minecraft.getInstance().player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.MASTER,1,1);

        ModStats stats = FileManager.getStats();
        if(isCorrect) FileManager.updateStats(stats.incrementCorrect());
        else FileManager.updateStats(stats.incrementWrong());
    }

    @Override
    public void init() {
        if(isCorrect) titleText.setPosition(width/2 - 57, 15);
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
        if(imageWidget != null) {
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
