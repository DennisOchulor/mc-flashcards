package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@Environment(EnvType.CLIENT)
public class QuestionScheduler {
    private QuestionScheduler() {}

    private static final List<Question> questions = new ArrayList<>();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static ModConfig config = FileManager.getConfig();
    private static ScheduledFuture<?> future = executor.schedule(() -> {}, 0, TimeUnit.SECONDS);
    public static long playerLastHurtTime = 0L;

    public static void reload() {
        config = FileManager.getConfig();
        questions.clear();
        FileManager.getQuestions().forEach((category,list) -> {
            if(config.categoryToggle().getOrDefault(category,true)) questions.addAll(list);
        });
        Collections.shuffle(questions);
    }

    public static void schedule() {
        if(!config.intervalToggle()) return;
        future.cancel(false);
        future = executor.schedule(QuestionScheduler::runTask,config.interval(),TimeUnit.MINUTES);
    }

    public static void stop() {
        future.cancel(false);
    }

    public static void updateConfig(ModConfig newConfig) {
        config = newConfig;
        future.cancel(true);
        if(newConfig.intervalToggle()) schedule();
    }

    public static void promptQuestion() {
        Objects.requireNonNull(Minecraft.getInstance().player); // sanity check to avoid questions outside a world

        if(questions.isEmpty()) reload();
        if(questions.isEmpty()) return; // there are no questions... so just do nothing
        int rand = ThreadLocalRandom.current().nextInt(0,questions.size());
        Question question = questions.remove(rand);
        if(config.validationToggle()) Minecraft.getInstance().setScreen(new AutoValidationQuestionScreen(question));
        else Minecraft.getInstance().setScreen(new ManualValidationQuestionScreen(question));
    }



    private static void runTask() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            ClientLevel level = Objects.requireNonNull(minecraft.level);
            boolean isAnotherScreenOpen = minecraft.screen != null;
            boolean wasPlayerHurtRecently = (level.getGameTime() - playerLastHurtTime) < (level.tickRateManager().tickrate() * 10); // 10 seconds
            if(isAnotherScreenOpen || wasPlayerHurtRecently) {
                executor.schedule(QuestionScheduler::runTask, 1, TimeUnit.SECONDS); // wait 1 second before trying again
            }
            else {
                promptQuestion();
            }
        });
    }

}
