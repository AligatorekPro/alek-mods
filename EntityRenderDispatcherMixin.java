package pl.alek.alekclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.alek.alekclient.AlekClient;

/** Ukrywanie graczy: inni gracze po prostu sie nie renderuja. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void alekclient$hidePlayers(Entity entity, Frustum frustum, double x, double y, double z,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (AlekClient.hidePlayers && entity instanceof PlayerEntity
                && entity != MinecraftClient.getInstance().player) {
            cir.setReturnValue(false);
        }
    }
}
