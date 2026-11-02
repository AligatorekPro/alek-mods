package pl.alek.alekclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlekClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("AlekClient");

    public static AlekConfig config;

    // Stan w czasie gry
    public static boolean zoomActive = false;
    public static float zoomCurrent = 1.0f;
    public static boolean fullbright = false;

    public static boolean freelookActive = false;
    public static float freelookYaw = 0f;
    public static float freelookPitch = 0f;
    public static float freelookDistance = 4.0f;
    private static Perspective previousPerspective = Perspective.FIRST_PERSON;

    private static KeyBinding zoomKey;
    private static KeyBinding freelookKey;
    private static KeyBinding fullbrightKey;
    private static KeyBinding hitboxKey;
    private static KeyBinding fovKey;
    private static KeyBinding fovUpKey;
    private static KeyBinding fovDownKey;

    @Override
    public void onInitializeClient() {
        config = AlekConfig.load();

        zoomKey = key("zoom", GLFW.GLFW_KEY_C);
        freelookKey = key("freelook", GLFW.GLFW_KEY_LEFT_ALT);
        fullbrightKey = key("fullbright", GLFW.GLFW_KEY_H);
        hitboxKey = key("hitboxes", GLFW.GLFW_KEY_J);
        fovKey = key("fov", GLFW.GLFW_KEY_K);
        fovUpKey = key("fov_up", GLFW.GLFW_KEY_PAGE_UP);
        fovDownKey = key("fov_down", GLFW.GLFW_KEY_PAGE_DOWN);

        ClientTickEvents.END_CLIENT_TICK.register(AlekClient::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(HitboxRenderer::render);
        // zeby przy zamknieciu gry w options.txt nie zapisala sie dziwna jasnosc
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> fullbright = false);

        LOGGER.info("Alek Client zaladowany");
    }

    private static KeyBinding key(String id, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.alekclient." + id, InputUtil.Type.KEYSYM, code, "category.alekclient"));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) {
            freelookActive = false;
            zoomActive = false;
            return;
        }

        zoomActive = zoomKey.isPressed();

        boolean wantFreelook = freelookKey.isPressed();
        if (wantFreelook && !freelookActive) {
            startFreelook(client);
        } else if (!wantFreelook && freelookActive) {
            stopFreelook(client);
        }

        while (fullbrightKey.wasPressed()) {
            fullbright = !fullbright;
            msg(client, "Fullbright: " + (fullbright ? "WŁ." : "WYŁ."));
        }
        while (hitboxKey.wasPressed()) {
            config.hitboxesEnabled = !config.hitboxesEnabled;
            config.save();
            msg(client, "Kolorowe hitboxy: " + (config.hitboxesEnabled ? "WŁ." : "WYŁ."));
        }
        while (fovKey.wasPressed()) {
            config.fovEnabled = !config.fovEnabled;
            config.save();
            msg(client, "Własny FOV (" + config.fov + "): " + (config.fovEnabled ? "WŁ." : "WYŁ."));
        }
        while (fovUpKey.wasPressed()) {
            changeFov(client, 5);
        }
        while (fovDownKey.wasPressed()) {
            changeFov(client, -5);
        }
    }

    private static void changeFov(MinecraftClient client, int delta) {
        config.fov = Math.max(30, Math.min(179, config.fov + delta));
        config.fovEnabled = true;
        config.save();
        msg(client, "FOV: " + config.fov);
    }

    private static void startFreelook(MinecraftClient client) {
        freelookYaw = client.player.getYaw();
        freelookPitch = client.player.getPitch();
        previousPerspective = client.options.getPerspective();
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        freelookActive = true;
    }

    private static void stopFreelook(MinecraftClient client) {
        freelookActive = false;
        client.options.setPerspective(previousPerspective);
    }

    private static void msg(MinecraftClient client, String text) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(text), true);
        }
    }
}
