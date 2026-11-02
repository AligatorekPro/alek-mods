package pl.alek.alekclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.player.PlayerEntity;
import pl.alek.alekclient.AlekClient;

/** Freelook (ruch myszka kreci kamera) i wolniejsza mysz podczas zooma. */
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

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double alekclient$slowX(double cursorDeltaX) {
        return alekclient$zoomScale(cursorDeltaX);
    }

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double alekclient$slowY(double cursorDeltaY) {
        return alekclient$zoomScale(cursorDeltaY);
    }

    /** Im wiekszy zoom, tym wolniejsza mysz. */
    private double alekclient$zoomScale(double value) {
        if (AlekClient.zoomCurrent < 0.999f && (Object) this == MinecraftClient.getInstance().player) {
            return value * Math.pow(AlekClient.zoomCurrent, 0.75);
        }
        return value;
    }

    /** Ukryci gracze nie blokuja celownika. */
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true, require = 0)
    private void alekclient$noHitHidden(CallbackInfoReturnable<Boolean> cir) {
        if (AlekClient.hidePlayers && (Object) this instanceof PlayerEntity
                && (Object) this != MinecraftClient.getInstance().player) {
            cir.setReturnValue(false);
        }
    }
}
