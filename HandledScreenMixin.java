package pl.alek.alekclient.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Dostep do slotu pod kursorem i pozycji okna ekwipunku (potrzebne do item locka). */
@Mixin(HandledScreen.class)
public interface HandledScreenMixin {

    @Accessor("focusedSlot")
    Slot alekclient$getFocusedSlot();

    @Accessor("x")
    int alekclient$getX();

    @Accessor("y")
    int alekclient$getY();
}
