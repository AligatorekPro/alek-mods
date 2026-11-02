package pl.alek.alekclient.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.alek.alekclient.AlekClient;

/** Kolko myszy: zmienia zoom (przy wcisnietym zoomie) albo odleglosc kamery (przy freelooku). */
@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void alekclient$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (AlekClient.config == null || vertical == 0.0) {
            return;
        }
        if (AlekClient.zoomActive) {
            double level = AlekClient.zoomLevel * (vertical > 0 ? 1.15 : 1.0 / 1.15);
            AlekClient.zoomLevel = Math.max(1.1, Math.min(100.0, level));
            ci.cancel();
        } else if (AlekClient.freelookActive) {
            // mnozenie zamiast dodawania: kamera oddala sie duzo szybciej
            float d = AlekClient.freelookDistance * (vertical > 0 ? 1.0f / 1.35f : 1.35f);
            AlekClient.freelookDistance = Math.max(1.0f, Math.min(40.0f, d));
            ci.cancel();
        }
    }
}
