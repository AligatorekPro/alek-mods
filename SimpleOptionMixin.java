package pl.alek.alekclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.alek.alekclient.AlekClient;

/** Fullbright: gdy wlaczony, opcja jasnosci (gamma) zwraca bardzo duza wartosc. */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin {

    @ModifyReturnValue(method = "getValue", at = @At("RETURN"))
    private Object alekclient$fullbright(Object original) {
        if (AlekClient.fullbright) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null && (Object) this == client.options.getGamma()) {
                return 15.0D;
            }
        }
        return original;
    }
}
