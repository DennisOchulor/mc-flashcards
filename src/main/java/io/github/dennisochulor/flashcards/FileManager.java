package io.github.dennisochulor.flashcards;

import com.google.gson.Gson;
import io.github.dennisochulor.flashcards.config.ModConfig;
import io.github.dennisochulor.flashcards.questions.Question;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class FileManager {
    private FileManager() {}

    private static final Path dotMinecraftFolder;
    private static final File configFile;
    private static final File questionsFile;
    private static final boolean isDev = true; //TODO

    static {
        try {
            dotMinecraftFolder = MinecraftClient.getInstance().runDirectory.toPath();

            configFile = new File(dotMinecraftFolder + "/config/flashcards/config.json");
            questionsFile = new File(dotMinecraftFolder + "/config/flashcards/questions.json");

            if(!configFile.exists() && !isDev) {
                Files.copy(FileManager.class.getResourceAsStream("/flashcards"), Path.of(dotMinecraftFolder + "/config/flashcards"));
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {} // run static initializer

    public static ModConfig getConfig() {
        try {
            return new Gson().fromJson(Files.readString(configFile.toPath()),ModConfig.class);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void updateConfig(ModConfig config) {
        try {
            FileWriter writer = new FileWriter(configFile);
            new Gson().toJson(config,ModConfig.class,writer);
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Question> getQuestions() {
        try {
            Question[] questions = new Gson().fromJson(Files.readString(questionsFile.toPath()),Question[].class);
            return List.of(questions);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
