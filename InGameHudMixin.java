package pl.alek.alekclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.alek.alekclient.AlekClient;
import pl.alek.alekclient.AlekConfig;

/** Zwykly celownik z gry, tylko w innym kolorze. Zmienia kolor, gdy celujesz w gracza. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Unique
    private boolean alekclient$tinted = false;

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void alekclient$tintStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        alekclient$tinted = false;
        AlekConfig cfg = AlekClient.config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || !cfg.customCrosshair || mc.options.hudHidden
                || !mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        boolean onPlayer = mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity;
        int c = onPlayer ? cfg.crosshairTargetArgb : cfg.crosshairArgb;
        context.draw(); // dokoncz to, co juz narysowane, zeby kolor dotyczyl tylko celownika
        RenderSystem.setShaderColor(((c >> 16) & 255) / 255.0f, ((c >> 8) & 255) / 255.0f, (c & 255) / 255.0f, 1.0f);
        alekclient$tinted = true;
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void alekclient$tintEnd(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (alekclient$tinted) {
            context.draw();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            alekclient$tinted = false;
        }
    }
}
