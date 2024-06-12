package io.github.dennisochulor.flashcards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.dennisochulor.flashcards.config.ModConfig;
import io.github.dennisochulor.flashcards.questions.Question;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Environment(EnvType.CLIENT)
public class FileManager {
    private FileManager() {}

    private static final Path dotMinecraftFolder;
    private static final File configFile;
    private static final File questionsFolder;
    private static final boolean isDev = true; //TODO

    static {
        try {
            dotMinecraftFolder = MinecraftClient.getInstance().runDirectory.toPath();

            configFile = new File(dotMinecraftFolder + "/config/flashcards/config.json");
            questionsFolder = new File(dotMinecraftFolder + "/config/flashcards/questions/");

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

    public static HashMap<String,List<Question>> getQuestions() {
        Gson gson = new GsonBuilder().create();
        HashMap<String,List<Question>> map = new HashMap<>();
        Arrays.stream(questionsFolder.listFiles()).forEachOrdered(f -> {
            Question[] questions;
            try {
                questions = gson.fromJson(Files.readString(f.toPath()), Question[].class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            map.put(f.getName().split("\\.")[0],new ArrayList<>(List.of(questions)));
        });
        return map;
    }

    public static void updateQuestions(HashMap<String,List<Question>> map) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Arrays.stream(questionsFolder.listFiles()).forEach(File::delete);

            for (Map.Entry<String, List<Question>> entry : map.entrySet()) {
                String filename = entry.getKey();
                List<Question> questions = entry.getValue();
                FileWriter writer = new FileWriter(questionsFolder.toPath() + "/" + filename + ".json");
                gson.toJson(questions.toArray(), Question[].class, writer);
                writer.flush();
                writer.close();
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
