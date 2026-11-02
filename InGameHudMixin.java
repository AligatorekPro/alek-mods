package pl.alek.alekclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.alek.alekclient.AlekClient;
import pl.alek.alekclient.AlekConfig;

/** Wlasny celownik w kolorze z configu. Zmienia kolor, gdy celujesz w gracza. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void alekclient$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AlekConfig cfg = AlekClient.config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || !cfg.customCrosshair || mc.options.hudHidden
                || !mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        boolean onPlayer = mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity;
        int color = onPlayer ? cfg.crosshairTargetArgb : cfg.crosshairArgb;

        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;
        int gap = 3;
        int len = 5;
        context.fill(cx - gap - len, cy, cx - gap, cy + 1, color);
        context.fill(cx + gap + 1, cy, cx + gap + 1 + len, cy + 1, color);
        context.fill(cx, cy - gap - len, cx + 1, cy - gap, color);
        context.fill(cx, cy + gap + 1, cx + 1, cy + gap + 1 + len, color);
        ci.cancel();
    }
}
