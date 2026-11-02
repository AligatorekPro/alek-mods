package pl.alek.alekclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.alek.alekclient.AlekClient;

/** Ukryci gracze nie blokuja celownika (da sie kopac i bic to, co jest za nimi). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true, require = 0)
    private void alekclient$noHitHidden(CallbackInfoReturnable<Boolean> cir) {
        if (AlekClient.hidePlayers && (Object) this instanceof PlayerEntity
                && (Object) this != MinecraftClient.getInstance().player) {
            cir.setReturnValue(false);
        }
    }
}
