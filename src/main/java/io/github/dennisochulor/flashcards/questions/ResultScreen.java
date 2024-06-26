package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.ModStats;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
class ResultScreen extends Screen {

    protected ResultScreen(Question question, String userAnswer) {
        super(Text.literal("Question Result"));
        this.question = question;
        this.isCorrect = question.answer().equalsIgnoreCase(userAnswer);
        this.userAnswer = userAnswer;
    }

    private TextWidget titleText;
    private TextWidget resultText;
    private ButtonWidget doneButton;
    private MultilineTextWidget questionText;
    private MultilineTextWidget yourAnswerText;
    private MultilineTextWidget correctAnswerText;
    private final Question question;
    private final boolean isCorrect;
    private final String userAnswer;

    @Override
    public void init() {
        titleText = new TextWidget(Text.literal("Your answer is "), MinecraftClient.getInstance().textRenderer);
        titleText.setPosition(width/2 - 50, 15);

        resultText = new TextWidget(Text.literal(isCorrect ? "CORRECT" : "WRONG"), MinecraftClient.getInstance().textRenderer);
        if(isCorrect) resultText.setTextColor(9498256); //green
        else resultText.setTextColor(16711680); //red
        resultText.setPosition(width/2 + 30, 15);

        questionText = new MultilineTextWidget(Text.literal("§n§lQuestion:§r\n" + question.question()), MinecraftClient.getInstance().textRenderer);
        questionText.setPosition(width/2 - Math.min(questionText.getWidth(), 250)/2, 45);
        questionText.setMaxWidth(250);
        questionText.setCentered(true);

        yourAnswerText = new MultilineTextWidget(Text.literal("§n§lYour answer:§r\n" + userAnswer), MinecraftClient.getInstance().textRenderer);
        yourAnswerText.setPosition(width/2 - Math.min(yourAnswerText.getWidth(), 250)/2, 115);
        yourAnswerText.setMaxWidth(250);
        yourAnswerText.setCentered(true);

        if(!isCorrect) {
            correctAnswerText = new MultilineTextWidget(Text.literal("§n§lCorrect answer:§r\n" + question.answer()), MinecraftClient.getInstance().textRenderer);
            correctAnswerText.setPosition(width/2 - Math.min(correctAnswerText.getWidth(), 250)/2, 165);
            correctAnswerText.setMaxWidth(250);
            correctAnswerText.setCentered(true);
            addDrawable(correctAnswerText);
        }

        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            QuestionScheduler.schedule();
            this.close();
            ModConfig config = FileManager.getConfig();

            // run correct/wrong commands if applicable
            if(MinecraftClient.getInstance().isIntegratedServerRunning()) {
                if(isCorrect) config.correctAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s run " + c));
                else config.wrongAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s run " + c));
            }
            else if(MinecraftClient.getInstance().player.hasPermissionLevel(2)) {
                if(isCorrect) config.correctAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s run " + c));
                else config.wrongAnswerCommands().forEach(c -> MinecraftClient.getInstance().getNetworkHandler().sendCommand("execute as @s run " + c));
            }
        }).dimensions(width/2 - 37,220,75,20).build();

        addDrawable(titleText);
        addDrawable(resultText);
        addDrawable(questionText);
        addDrawableChild(doneButton);
        addDrawable(yourAnswerText);

        if(isCorrect) MinecraftClient.getInstance().player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER,1,1);
        else MinecraftClient.getInstance().player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.MASTER,1,1);

        ModStats stats = FileManager.getStats();
        if(isCorrect) FileManager.updateStats(stats.incrementCorrect());
        else FileManager.updateStats(stats.incrementWrong());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
