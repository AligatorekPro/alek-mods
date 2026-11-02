package pl.alek.alekclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.alek.alekclient.AlekClient;
import pl.alek.alekclient.AlekConfig;

/** Wlasny FOV (do 179) i zoom. Dziala tylko na FOV swiata, nie na rece. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float alekclient$fov(float original, Camera camera, float tickDelta, boolean changingFov) {
        AlekConfig cfg = AlekClient.config;
        if (cfg == null || !changingFov) {
            return original;
        }

        float result = original;

        if (cfg.fovEnabled) {
            float optionFov = MinecraftClient.getInstance().options.getFov().getValue().floatValue();
            if (optionFov > 0.0f) {
                result = original * (cfg.fov / optionFov);
            }
        }

        float target = AlekClient.zoomActive ? (float) (1.0 / cfg.zoomFactor) : 1.0f;
        AlekClient.zoomCurrent += (target - AlekClient.zoomCurrent) * 0.25f;
        if (Math.abs(AlekClient.zoomCurrent - 1.0f) > 0.001f) {
            result *= AlekClient.zoomCurrent;
        }

        return Math.min(result, 179.0f);
    }
}
