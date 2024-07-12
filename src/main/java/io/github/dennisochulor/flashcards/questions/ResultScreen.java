package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.Utils;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.IconWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
class ResultScreen extends Screen {

    private final TextWidget titleText = new TextWidget(Text.literal("Your answer is "), MinecraftClient.getInstance().textRenderer);
    private final TextWidget resultText;
    private final ButtonWidget doneButton;
    private final MultilineTextWidget questionText;
    private final MultilineTextWidget yourAnswerText;
    private final MultilineTextWidget correctAnswerText;
    private final IconWidget imageWidget;
    private final boolean isCorrect;

    protected ResultScreen(Question question, String userAnswer) {
        super(Text.literal("Question Result"));
        isCorrect = question.answer().equalsIgnoreCase(userAnswer);

        resultText = new TextWidget(Text.literal(isCorrect ? "CORRECT" : "WRONG"), MinecraftClient.getInstance().textRenderer);
        if(isCorrect) resultText.setTextColor(Colors.GREEN);
        else resultText.setTextColor(Colors.LIGHT_RED);

        questionText = new MultilineTextWidget(Text.literal("§n§lQuestion:§r\n" + question.question()), MinecraftClient.getInstance().textRenderer);

        yourAnswerText = new MultilineTextWidget(Text.literal("§n§lYour answer:§r\n" + userAnswer), MinecraftClient.getInstance().textRenderer);

        correctAnswerText  = new MultilineTextWidget(Text.literal("§n§lCorrect answer:§r\n" + question.answer()), MinecraftClient.getInstance().textRenderer);

        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            QuestionScheduler.schedule();
            this.close();
            ModConfig config = FileManager.getConfig();

            // run correct/wrong commands if applicable
            if(MinecraftClient.getInstance().isIntegratedServerRunning()) {
                if(isCorrect) config.correctAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + c));
                else config.wrongAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + c));
            }
            else if(MinecraftClient.getInstance().player.hasPermissionLevel(2)) {
                if(isCorrect) config.correctAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + c));
                else config.wrongAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s at @s run " + c));
            }
        }).build();

        if(question.imageName() != null) {
            Identifier id = Utils.getImageId(FileManager.getImage(question.imageName()));
            if(id == null) {
                imageWidget = IconWidget.create(140,140,Identifier.ofVanilla("textures/missing.png"),140,140);
                imageWidget.setTooltip(Tooltip.of(Text.literal(question.imageName() + " seems to be missing...")));
            }
            else imageWidget = IconWidget.create(140,140,id,140,140);
        }
        else imageWidget = null;

        if(isCorrect) MinecraftClient.getInstance().player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER,1,1);
        else MinecraftClient.getInstance().player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.MASTER,1,1);

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

        doneButton.setDimensionsAndPosition(75,20,width/2 - 37,220);

        addDrawable(titleText);
        addDrawable(resultText);
        addDrawable(questionText);
        addDrawable(yourAnswerText);
        addDrawable(correctAnswerText);
        addDrawableChild(doneButton);
        if(imageWidget != null) {
            addDrawable(imageWidget);
            questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2 + 100, 45);
            imageWidget.setPosition(width/4 - 75,25);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
