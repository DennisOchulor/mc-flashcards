package io.github.dennisochulor.flashcards.questions;

import io.github.dennisochulor.flashcards.FileManager;
import io.github.dennisochulor.flashcards.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

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

    public static void reload() {
        questions.clear();
        FileManager.getQuestions().values().forEach(questions::addAll);
        Collections.shuffle(questions);
        schedule();
    }

    public static void schedule() {
        if(questions.isEmpty()) {
            reload();
            return;
        }
        if(future != null) future.cancel(false);

        future = executor.schedule(() -> {
            if(MinecraftClient.getInstance().world == null) {schedule(); return;}
            if(!config.intervalToggle()) {schedule(); return;}

            while(MinecraftClient.getInstance().currentScreen != null) {
                try {
                    Thread.sleep(1000); //wait till no screens are open...
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            MinecraftClient.getInstance().execute(() -> {
                int rand = ThreadLocalRandom.current().nextInt(0,questions.size());
                Question question = questions.remove(rand);
                MinecraftClient.getInstance().setScreen(new QuestionScreen(question));
            });
        },config.interval(),TimeUnit.MINUTES);
    }

    public static void updateConfig(ModConfig newConfig) {
        config = newConfig;
        schedule();
    }

}
