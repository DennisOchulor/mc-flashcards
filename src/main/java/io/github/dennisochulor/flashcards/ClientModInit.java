package io.github.dennisochulor.flashcards;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.dennisochulor.flashcards.config.*;
import io.github.dennisochulor.flashcards.questions.QuestionScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.UIManager;

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
        ClientPlayConnectionEvents.JOIN.register((_, _, _) -> QuestionScheduler.schedule());
        ClientPlayConnectionEvents.DISCONNECT.register((_,_) -> QuestionScheduler.stop());
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if(minecraft.player == null || minecraft.level == null) return;
            if(minecraft.player.hurtTime != 0) QuestionScheduler.playerLastHurtTime = minecraft.level.getGameTime();
        });

        KeyMapping.Category keyBindingCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        KeyMapping keyBindingConfigMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Flashcards Config Menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                keyBindingCategory
        ));

        KeyMapping keyBindingPromptQuestion = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Prompt a question",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                keyBindingCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if(keyBindingConfigMenu.consumeClick()) {
                //noinspection StatementWithEmptyBody
                while(keyBindingConfigMenu.consumeClick()); //consume additional presses

                if(minecraft.screen instanceof ConfigurationScreen) {
                    minecraft.screen.onClose();
                }
                else if(minecraft.screen == null) {
                    ConfigurationScreen screen = new ConfigurationScreen();
                    minecraft.setScreen(screen);
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if(keyBindingPromptQuestion.consumeClick()) {
                //noinspection StatementWithEmptyBody
                while(keyBindingPromptQuestion.consumeClick()); //consume additional presses

                if(minecraft.player == null || minecraft.level == null) return;
                if(FileManager.getConfig().intervalToggle()) {
                    MutableComponent text = Component.literal("The interval toggle must be off for you to prompt a question on-demand.").withColor(CommonColors.SOFT_RED);
                    minecraft.player.displayClientMessage(text,true);
                    return;
                }
                QuestionScheduler.promptQuestion();
            }
        });
    }

}
