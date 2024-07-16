package io.github.dennisochulor.flashcards;

import io.github.dennisochulor.flashcards.config.*;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class ClientModInit implements ClientModInitializer {

    /* todo fml
    - fix bug where image just randomly doesnt work on QuestionAdd/EditScreen + removeButton issue
     */

    public static final String MOD_ID = "flashcards";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless","false"); // needed for question image chooser to function
        LOGGER.info("Initializing flashcards client");
        FileManager.init();
        QuestionScheduler.reload();
        ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler,sender,client) -> QuestionScheduler.schedule());
        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler,client) -> QuestionScheduler.stop());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.player == null) return;
            if(client.player.hurtTime != 0) QuestionScheduler.playerLastHurtTime = client.world.getTime();
        });

        KeyBinding keyBindingConfigMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Flashcards Config Menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "Flashcards Mod"
        ));

        KeyBinding keyBindingPromptQuestion =  KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Prompt a question",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "Flashcards Mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(keyBindingConfigMenu.wasPressed()) {
                if(client.currentScreen instanceof ConfigurationScreen) {
                    client.currentScreen.close();
                }
                else if(client.currentScreen == null) {
                    ConfigurationScreen screen = new ConfigurationScreen();
                    client.setScreen(screen);
                }
            }
            while(keyBindingConfigMenu.wasPressed()); //consume additional presses
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(keyBindingPromptQuestion.wasPressed()) {
                while(keyBindingPromptQuestion.wasPressed()); //consume additional presses
                if(MinecraftClient.getInstance().world == null) return;
                if(FileManager.getConfig().intervalToggle()) {
                    MutableText text = Text.literal("The interval toggle must be off for you to prompt a question on-demand.").withColor(Colors.LIGHT_RED);
                    MinecraftClient.getInstance().player.sendMessage(text,true);
                    return;
                }
                QuestionScheduler.promptQuestion();
            }
        });
    }

}
