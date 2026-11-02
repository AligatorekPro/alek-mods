package pl.alek.alekclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.alek.alekclient.AlekClient;
import pl.alek.alekclient.ItemLock;

/** Item lock: zablokowany slot jest zamrozony (nie wyrzucisz, nie przeniesiesz, nie zamienisz). Do tego brak bicia ukrytych graczy. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void alekclient$lockedSlots(int syncId, int slotId, int button, SlotActionType actionType,
                                        PlayerEntity player, CallbackInfo ci) {
        boolean blocked = false;
        switch (actionType) {
            case THROW, QUICK_MOVE, PICKUP -> blocked = ItemLock.isLockedSlot(player, slotId);
            // SWAP: "button" to numer slotu paska (0-8) albo 40 dla drugiej reki
            case SWAP -> blocked = ItemLock.isLockedSlot(player, slotId) || ItemLock.isLockedIndex(button);
            default -> {
            }
        }
        if (blocked) {
            player.sendMessage(Text.literal("Ten slot jest zablokowany"), true);
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
