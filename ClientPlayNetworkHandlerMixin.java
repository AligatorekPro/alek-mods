package pl.alek.alekclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.alek.alekclient.ItemLock;

/** Item lock: klawisz F (zamiana rak) nie ruszy zablokowanej drugiej reki (np. totemu) ani zablokowanego slotu w rece. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void alekclient$noSwapLocked(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof PlayerActionC2SPacket action
                && action.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null
                    && (ItemLock.isLockedIndex(40) || ItemLock.isLockedIndex(mc.player.getInventory().selectedSlot))) {
                mc.player.sendMessage(Text.literal("Ten slot jest zablokowany"), true);
                ci.cancel();
            }
        }
    }
}
