package pl.alek.alekclient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Gdy gracze sa ukryci, pokazuje same nicki w miejscu, gdzie stoja.
 * Nicki sa zwyklym tekstem na ekranie, nie ma ich wiec za sciana tylko tam, gdzie i tak byloby widac gracza.
 */
public class NickLabels {

    private record Label(String name, float nx, float ny) {
    }

    private static List<Label> labels = new ArrayList<>();

    public static void clear() {
        labels = new ArrayList<>();
    }

    /** Wywolywane przy renderowaniu swiata: liczy, gdzie na ekranie sa glowy graczy. */
    public static void collect(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Label> out = new ArrayList<>();

        if (AlekClient.hidePlayers && mc.world != null) {
            Vec3d cam = ctx.camera().getPos();
            float tickDelta = ctx.tickCounter().getTickDelta(false);
            Matrix4f view = new Matrix4f().rotation(ctx.camera().getRotation().conjugate(new Quaternionf()));
            Matrix4f viewProjection = new Matrix4f(ctx.projectionMatrix()).mul(view);

            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) {
                    continue;
                }
                Vec3d pos = p.getLerpedPos(tickDelta).add(0, p.getHeight() + 0.3, 0).subtract(cam);
                if (pos.lengthSquared() > 128.0 * 128.0) {
                    continue;
                }
                Vector4f v = new Vector4f((float) pos.x, (float) pos.y, (float) pos.z, 1.0f);
                viewProjection.transform(v);
                if (v.w <= 0.1f) {
                    continue; // za plecami
                }
                float nx = v.x / v.w;
                float ny = v.y / v.w;
                if (Math.abs(nx) > 1.1f || Math.abs(ny) > 1.1f) {
                    continue;
                }
                out.add(new Label(p.getName().getString(), nx * 0.5f + 0.5f, 0.5f - ny * 0.5f));
            }
        }
        labels = out;
    }

    public static void draw(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!AlekClient.hidePlayers || mc.options.hudHidden || mc.currentScreen != null) {
            return;
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        for (Label l : labels) {
            ctx.drawCenteredTextWithShadow(mc.textRenderer, l.name(), (int) (l.nx() * w), (int) (l.ny() * h), 0xFFFFFFFF);
        }
    }
}
