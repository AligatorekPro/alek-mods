package pl.alek.alekclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ustawienia moda. Plik: .minecraft/config/alekclient.json
 * Kolory podajesz jako HEX, np. "#FF69B4".
 */
public class AlekConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("alekclient.json");

    // Wersja configu (do automatycznych zmian domyslnych wartosci)
    public int configVersion = 0;

    // Zoom
    public double zoomFactor = 10.0;

    // Wlasny FOV (30 - 179)
    public boolean fovEnabled = true;
    public int fov = 135;

    // Hitboxy
    public boolean hitboxesEnabled = false;
    public String playerHitboxColor = "#FF69B4";   // rozowy
    public String animalHitboxColor = "#FFDD00";   // zolty
    public String mobHitboxColor = "#FF0000";      // czerwony
    public String itemHitboxColor = "#3399FF";     // niebieski
    public String otherHitboxColor = "#FFFFFF";    // reszta

    // Overlay klawiszy i celownik
    public boolean keyOverlay = true;
    public boolean customCrosshair = true;
    public String crosshairColor = "#FF69B4";        // rozowy
    public String crosshairTargetColor = "#FF0000";  // gdy celujesz w gracza

    public transient int crosshairArgb;
    public transient int crosshairTargetArgb;

    public transient float[] playerRgb;
    public transient float[] animalRgb;
    public transient float[] mobRgb;
    public transient float[] itemRgb;
    public transient float[] otherRgb;

    public static AlekConfig load() {
        AlekConfig cfg = new AlekConfig();
        if (Files.exists(PATH)) {
            try {
                AlekConfig read = GSON.fromJson(Files.readString(PATH), AlekConfig.class);
                if (read != null) {
                    cfg = read;
                }
            } catch (Exception e) {
                AlekClient.LOGGER.warn("Nie udalo sie wczytac configu, uzywam domyslnego", e);
            }
        }
        if (cfg.configVersion < 2) {
            cfg.zoomFactor = 10.0;
            cfg.fov = 135;
            cfg.fovEnabled = true;
            cfg.configVersion = 2;
        }
        cfg.refresh();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            AlekClient.LOGGER.warn("Nie udalo sie zapisac configu", e);
        }
    }

    public void refresh() {
        zoomFactor = Math.max(1.1, Math.min(100.0, zoomFactor));
        fov = Math.max(30, Math.min(179, fov));
        crosshairArgb = argb(crosshairColor, 0xFFFF69B4);
        crosshairTargetArgb = argb(crosshairTargetColor, 0xFFFF0000);
        playerRgb = parse(playerHitboxColor, new float[]{1.0f, 0.41f, 0.71f});
        animalRgb = parse(animalHitboxColor, new float[]{1.0f, 0.87f, 0.0f});
        mobRgb = parse(mobHitboxColor, new float[]{1.0f, 0.0f, 0.0f});
        itemRgb = parse(itemHitboxColor, new float[]{0.2f, 0.6f, 1.0f});
        otherRgb = parse(otherHitboxColor, new float[]{1.0f, 1.0f, 1.0f});
    }

    private static int argb(String hex, int fallback) {
        try {
            return 0xFF000000 | Integer.parseInt(hex.replace("#", "").trim(), 16);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float[] parse(String hex, float[] fallback) {
        try {
            int c = Integer.parseInt(hex.replace("#", "").trim(), 16);
            return new float[]{((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f};
        } catch (Exception e) {
            return fallback;
        }
    }
}
