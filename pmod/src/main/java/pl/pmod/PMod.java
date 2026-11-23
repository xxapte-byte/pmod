package pl.pmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pl.pmod.mixin.MinecraftClientAccessor;

public class PMod implements ClientModInitializer {
    public static KeyBinding OPEN_GUI;

    @Override
    public void onInitializeClient() {
        Config.load();
        OPEN_GUI = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.pmod.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.pmod"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_GUI.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ConfigScreen(null));
            }
            if (Config.get().fastPlace && mc.player != null) {
                ((MinecraftClientAccessor) mc).pmod$setItemUseCooldown(0);
            }
        });

        WorldRenderEvents.LAST.register(Renderer::render);
    }
}
