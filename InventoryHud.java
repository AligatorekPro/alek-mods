package pl.alek.alekclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/** Podglad ekwipunku (3 rzedy po 9) na ekranie z lewej strony, bez otwierania ekwipunku. */
public class InventoryHud {

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        AlekConfig cfg = AlekClient.config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || !cfg.inventoryHud || mc.options.hudHidden || mc.currentScreen != null || mc.player == null) {
            return;
        }

        float scale = (float) cfg.inventoryHudScale;
        int cols = 9;
        int rows = 3;
        int cell = 18;
        int pad = 3;
        int w = cols * cell + pad * 2;
        int h = rows * cell + pad * 2;

        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1.0f);

        int x0 = 4;
        int y0 = (int) (ctx.getScaledWindowHeight() / scale / 2.0f - h / 2.0f);
        ctx.fill(x0, y0, x0 + w, y0 + h, 0x80000000);

        PlayerInventory inv = mc.player.getInventory();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = 9 + r * cols + c; // sloty 9-35 to glowny ekwipunek
                ItemStack stack = inv.getStack(index);
                int ix = x0 + pad + c * cell + 1;
                int iy = y0 + pad + r * cell + 1;
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, ix, iy);
                    ctx.drawStackOverlay(mc.textRenderer, stack, ix, iy);
                }
                if (ItemLock.isLockedIndex(index)) {
                    ItemLock.drawLock(ctx, ix, iy);
                }
            }
        }

        ctx.getMatrices().pop();
    }
}
