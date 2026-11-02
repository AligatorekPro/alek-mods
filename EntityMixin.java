package pl.alek.alekclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.alek.alekclient.AlekClient;

/** Freelook: ruch myszka kreci kamera, a nie graczem. */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void alekclient$freelook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (AlekClient.freelookActive && (Object) this == MinecraftClient.getInstance().player) {
            AlekClient.freelookYaw += (float) cursorDeltaX * 0.15f;
            AlekClient.freelookPitch = MathHelper.clamp(AlekClient.freelookPitch + (float) cursorDeltaY * 0.15f, -90.0f, 90.0f);
            ci.cancel();
        }
    }
}
