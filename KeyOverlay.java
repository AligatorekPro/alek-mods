package pl.alek.alekclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;
import java.util.function.BooleanSupplier;

/** Okienka w prawym gornym rogu: pod jakim klawiszem masz funkcje moda. Wlaczona funkcja swieci sie na rozowo. */
public class KeyOverlay {

    private record Entry(KeyBinding key, String label, BooleanSupplier active) {
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        AlekConfig cfg = AlekClient.config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || !cfg.keyOverlay || mc.options.hudHidden || mc.currentScreen != null || mc.player == null) {
            return;
        }

        List<Entry> entries = List.of(
                new Entry(AlekClient.zoomKey, "Zoom", () -> AlekClient.zoomActive),
                new Entry(AlekClient.freelookKey, "Freelook", () -> AlekClient.freelookActive),
                new Entry(AlekClient.fullbrightKey, "Fullbright", () -> AlekClient.fullbright),
                new Entry(AlekClient.hitboxKey, "Hitboxy", () -> cfg.hitboxesEnabled),
                new Entry(AlekClient.fovKey, "FOV", () -> cfg.fovEnabled),
                new Entry(AlekClient.itemLockKey, "Item lock", () -> AlekClient.itemLock),
                new Entry(AlekClient.hidePlayersKey, "Ukryj graczy", () -> AlekClient.hidePlayers)
        );

        TextRenderer tr = mc.textRenderer;
        String[] lines = new String[entries.size()];
        int max = 0;
        for (int i = 0; i < lines.length; i++) {
            Entry e = entries.get(i);
            lines[i] = "[" + e.key().getBoundKeyLocalizedText().getString() + "] " + e.label();
            max = Math.max(max, tr.getWidth(lines[i]));
        }

        int boxW = max + 8;
        int x = ctx.getScaledWindowWidth() - boxW - 4;
        int y = 4;
        for (int i = 0; i < lines.length; i++) {
            boolean on = entries.get(i).active().getAsBoolean();
            ctx.fill(x, y, x + boxW, y + 12, on ? 0xB0FF69B4 : 0x90000000);
            ctx.drawTextWithShadow(tr, lines[i], x + 4, y + 2, 0xFFFFFFFF);
            y += 14;
        }
    }
}
