package io.github.dennisochulor.flashcards;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.dennisochulor.flashcards.config.*;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

@Environment(EnvType.CLIENT)
public class ClientModInit implements ClientModInitializer {

    public static final String MOD_ID = "flashcards";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless","false"); // needed for question image chooser to function
        try { // still isn't truly native looking but it is the best we got...
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Initializing flashcards client");
        FileManager.init();
        QuestionScheduler.reload();
        ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler,sender,client) -> QuestionScheduler.schedule());
        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler,client) -> QuestionScheduler.stop());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.player == null) return;
            if(client.player.hurtTime != 0) QuestionScheduler.playerLastHurtTime = client.level.getGameTime();
        });

        KeyMapping.Category keyBindingCategory = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"));

        KeyMapping keyBindingConfigMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Flashcards Config Menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                keyBindingCategory
        ));

        KeyMapping keyBindingPromptQuestion =  KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Prompt a question",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                keyBindingCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(keyBindingConfigMenu.consumeClick()) {
                if(client.screen instanceof ConfigurationScreen) {
                    client.screen.onClose();
                }
                else if(client.screen == null) {
                    ConfigurationScreen screen = new ConfigurationScreen();
                    client.setScreen(screen);
                }
            }
            while(keyBindingConfigMenu.consumeClick()); //consume additional presses
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(keyBindingPromptQuestion.consumeClick()) {
                while(keyBindingPromptQuestion.consumeClick()); //consume additional presses
                if(Minecraft.getInstance().level == null) return;
                if(FileManager.getConfig().intervalToggle()) {
                    MutableComponent text = Component.literal("The interval toggle must be off for you to prompt a question on-demand.").withColor(CommonColors.SOFT_RED);
                    Minecraft.getInstance().player.displayClientMessage(text,true);
                    return;
                }
                QuestionScheduler.promptQuestion();
            }
        });
    }

}
