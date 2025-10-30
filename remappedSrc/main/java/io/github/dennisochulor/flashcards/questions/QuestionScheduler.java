package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Environment(EnvType.CLIENT)
public class QuestionScheduler {
    private QuestionScheduler() {}

    private static final List<Question> questions = new ArrayList<>();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static ModConfig config = FileManager.getConfig();
    private static ScheduledFuture<?> future;
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
        if(config.intervalToggle() == false) return;
        if(future != null) future.cancel(false);

        future = executor.schedule(() -> {
            while(Minecraft.getInstance().screen != null || (Minecraft.getInstance().level.getGameTime() - playerLastHurtTime) < 200) {
                try {
                    Thread.sleep(1000); //wait till no screens are open...
                } catch (InterruptedException ignored) {
                    return;
                }
            }
            Minecraft.getInstance().execute(QuestionScheduler::promptQuestion);
        },config.interval(),TimeUnit.MINUTES);
    }

    public static void stop() {
        if(future != null) future.cancel(false);
    }

    public static void updateConfig(ModConfig newConfig) {
        config = newConfig;
        if(future != null) future.cancel(true);
        if(newConfig.intervalToggle()) schedule();
    }

    public static void promptQuestion() {
        if(questions.isEmpty()) reload();
        if(questions.isEmpty()) return; // there are no questions... so just do nothing
        int rand = ThreadLocalRandom.current().nextInt(0,questions.size());
        Question question = questions.remove(rand);
        if(config.validationToggle()) Minecraft.getInstance().setScreen(new AutoValidationQuestionScreen(question));
        else Minecraft.getInstance().setScreen(new ManualValidationQuestionScreen(question));
    }

}
