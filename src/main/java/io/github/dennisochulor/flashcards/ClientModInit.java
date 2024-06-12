package io.github.dennisochulor.flashcards;

import io.github.dennisochulor.flashcards.config.*;
import io.github.dennisochulor.flashcards.questions.Question;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import io.github.dennisochulor.flashcards.questions.QuestionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ClientModInit implements ClientModInitializer {

    public static final String MOD_ID = "flashcards";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing flashcards client");
        FileManager.init();
        QuestionScheduler.schedule();

        final KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Flashcards Config Menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "Flashcards Mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(keyBinding.wasPressed()) {
                if(client.currentScreen instanceof ConfigurationScreen) {
                    client.currentScreen.close();
                }
                else if(client.currentScreen == null) {
                    ConfigurationScreen screen = new ConfigurationScreen();
                    client.setScreen(screen);
                }
            }
        });
    }

}
