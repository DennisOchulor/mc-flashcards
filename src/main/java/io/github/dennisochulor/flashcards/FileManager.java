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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class FileManager {
    private FileManager() {}

    private static final Path dotMinecraftFolder;
    private static final File configFile;
    private static final File questionsFolder;

    static {
        try {
            dotMinecraftFolder = MinecraftClient.getInstance().runDirectory.toPath();

            configFile = new File(dotMinecraftFolder + "/config/flashcards/config.json");
            questionsFolder = new File(dotMinecraftFolder + "/config/flashcards/questions/");

            if(!questionsFolder.exists()) {
                questionsFolder.mkdirs();
                Files.copy(FileManager.class.getResourceAsStream("/flashcards/config.json"),Path.of(dotMinecraftFolder + "/config/flashcards/config.json"));
                Files.copy(FileManager.class.getResourceAsStream("/flashcards/questions/default.json"),Path.of(questionsFolder + "/default.json"));
            }
            importQuestions();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {} // run static initializer

    public static ModConfig getConfig() {
        try {
            // ensure questions and config are always in sync, otherwise kaboom
            ModConfig config = new Gson().fromJson(Files.readString(configFile.toPath()),ModConfig.class);
            AtomicBoolean changed = new AtomicBoolean(false);
            getQuestions().forEach((category,questions) -> {
                if(!config.categoryToggle().containsKey(category)) {
                    config.categoryToggle().put(category,true);
                    changed.set(true);
                }
            });
            if(changed.get()) updateConfig(config);

            return config;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void updateConfig(ModConfig config) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(configFile);
            gson.toJson(config,ModConfig.class,writer);
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
        Arrays.stream(questionsFolder.listFiles()).filter(f -> f.getName().endsWith(".json")).forEachOrdered(f -> {
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
            Arrays.stream(questionsFolder.listFiles()).filter(f -> f.getName().endsWith(".json")).forEach(File::delete);

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

    private static void importQuestions() {
        File[] files = questionsFolder.listFiles();
        Stream<File> stream = Arrays.stream(files).filter(f -> f.getName().endsWith(".txt"));
        importAnki(stream.filter(f -> f.getName().contains("anki")));
    }

    private static void importAnki(Stream<File> stream) {
        stream.forEach(f -> {
            try {
                List<Question> importedQuestions = new ArrayList<>();
                List<String> lines = Files.readAllLines(f.toPath());

                lines.stream().filter(line -> !line.startsWith("#")).forEach(line -> {
                    String[] q =  line.split("\t",2);
                    importedQuestions.add(new Question(q[0],q[1]));
                });

                String filename = "anki-" + ThreadLocalRandom.current().nextInt(0,1000);
                FileWriter writer = new FileWriter(questionsFolder.toPath() + "/" + filename + ".json");
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(importedQuestions.toArray(), Question[].class, writer);
                writer.flush();
                writer.close();

                f.delete();
            }
            catch (Exception e) {
                ClientModInit.LOGGER.warn(f.getName() + " encountered an error during import!", e);
            }
        });
    }

}
