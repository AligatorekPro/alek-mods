package pl.alek.alekclient;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Arm;
import pl.alek.alekclient.mixin.HandledScreenMixin;

/**
 * Item lock na poziomie slotow: w ekwipunku najedz na przedmiot i wcisnij klawisz (domyslnie O).
 * Dziala na kazdy slot: ekwipunek, hotbar, zbroja i druga reka (totem).
 * Zablokowany slot ma rozowa ramke z mala klodka i jest "zamrozony": nie wyrzucisz go, nie przeniesiesz,
 * nie zamienisz klawiszem F ani cyframi.
 */
public class ItemLock {

    public static boolean isLockedIndex(int index) {
        return AlekClient.config != null && AlekClient.config.lockedSlots.contains(index);
    }

    /** Zwraca true, jesli po zmianie slot jest zablokowany. */
    public static boolean toggleIndex(int index) {
        AlekConfig cfg = AlekClient.config;
        boolean nowLocked;
        if (cfg.lockedSlots.contains(index)) {
            cfg.lockedSlots.remove(Integer.valueOf(index));
            nowLocked = false;
        } else {
            cfg.lockedSlots.add(index);
            nowLocked = true;
        }
        cfg.save();
        return nowLocked;
    }

    /** Czy slot o danym numerze w otwartym oknie to zablokowany slot z ekwipunku gracza. */
    public static boolean isLockedSlot(PlayerEntity player, int slotId) {
        if (player == null || AlekClient.config == null) {
            return false;
        }
        var slots = player.currentScreenHandler.slots;
        if (slotId < 0 || slotId >= slots.size()) {
            return false;
        }
        Slot slot = slots.get(slotId);
        return slot.inventory == player.getInventory() && isLockedIndex(slot.getIndex());
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?> hs && !(screen instanceof CreativeInventoryScreen)) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((s, key, scancode, modifiers) -> {
                    if (hs.getFocused() instanceof TextFieldWidget) {
                        return; // pisanie w polu tekstowym (np. kowadlo) nie blokuje przedmiotow
                    }
                    if (AlekClient.itemLockKey.matchesKey(key, scancode)) {
                        Slot slot = ((HandledScreenMixin) (Object) hs).alekclient$getFocusedSlot();
                        if (slot != null && slot.inventory instanceof PlayerInventory) {
                            toggleIndex(slot.getIndex());
                        }
                    }
                });
                ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, tickDelta) -> {
                    HandledScreenMixin acc = (HandledScreenMixin) (Object) hs;
                    int ox = acc.alekclient$getX();
                    int oy = acc.alekclient$getY();
                    for (Slot slot : hs.getScreenHandler().slots) {
                        if (slot.inventory instanceof PlayerInventory && isLockedIndex(slot.getIndex())) {
                            drawLock(ctx, ox + slot.x, oy + slot.y);
                        }
                    }
                });
            }
        });
    }

    /** Zablokowane sloty paska szybkiego dostepu (hotbar) i druga reka tez maja ramke. */
    public static void renderHotbar(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (AlekClient.config == null || mc.player == null || mc.options.hudHidden || mc.currentScreen != null) {
            return;
        }
        int center = ctx.getScaledWindowWidth() / 2;
        int y = ctx.getScaledWindowHeight() - 16 - 3;
        for (int i = 0; i < 9; i++) {
            if (isLockedIndex(i)) {
                drawLock(ctx, center - 90 + 2 + i * 20, y);
            }
        }
        if (isLockedIndex(40)) {
            int x = mc.player.getMainArm() == Arm.RIGHT ? center - 91 - 26 : center + 91 + 10;
            drawLock(ctx, x, y);
        }
    }

    /** Rozowa ramka + mala klodka w rogu. Rysowane wysoko nad przedmiotem, zeby nic jej nie zaslanialo. */
    static void drawLock(DrawContext ctx, int x, int y) {
        int pink = 0xFFFF69B4;
        int white = 0xFFFFFFFF;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0.0f, 0.0f, 450.0f);

        // sama ramka
        ctx.fill(x, y, x + 16, y + 1, pink);
        ctx.fill(x, y + 15, x + 16, y + 16, pink);
        ctx.fill(x, y, x + 1, y + 16, pink);
        ctx.fill(x + 15, y, x + 16, y + 16, pink);

        // mala klodka w prawym dolnym rogu
        int lx = x + 10;
        int ly = y + 10;
        ctx.fill(lx, ly + 2, lx + 5, ly + 5, white);
        ctx.fill(lx + 1, ly, lx + 4, ly + 1, white);
        ctx.fill(lx + 1, ly + 1, lx + 2, ly + 2, white);
        ctx.fill(lx + 3, ly + 1, lx + 4, ly + 2, white);

        ctx.getMatrices().pop();
    }
}
