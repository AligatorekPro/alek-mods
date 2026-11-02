package pl.alek.alekclient.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.alek.alekclient.ItemLock;

/** Item lock: klawisz Q nie wyrzuca przedmiotu z zablokowanego slotu. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void alekclient$noDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (ItemLock.isLockedIndex(self.getInventory().selectedSlot)) {
            self.sendMessage(Text.literal("Ten przedmiot jest zablokowany"), true);
            cir.setReturnValue(false);
        }
    }
}
