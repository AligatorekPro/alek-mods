package pl.alek.alekclient.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.alek.alekclient.AlekClient;

/** Item lock: klawisz Q nie wyrzuca przedmiotow. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void alekclient$noDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (AlekClient.itemLock) {
            cir.setReturnValue(false);
        }
    }
}
