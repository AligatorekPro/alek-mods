package pl.alek.alekclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.alek.alekclient.AlekClient;

/** Item lock w ekwipunku (Q na slocie i klikniecie poza oknem) oraz brak bicia ukrytych graczy. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void alekclient$noThrow(int syncId, int slotId, int button, SlotActionType actionType,
                                    PlayerEntity player, CallbackInfo ci) {
        if (AlekClient.itemLock && (actionType == SlotActionType.THROW || slotId == -999)) {
            ci.cancel();
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void alekclient$noHitHidden(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (AlekClient.hidePlayers && target instanceof PlayerEntity) {
            ci.cancel();
        }
    }
}
