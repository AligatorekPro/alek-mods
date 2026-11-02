package pl.alek.alekclient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Rysuje hitboxy w kolorach zaleznych od typu bytu.
 * Linie sa rysowane z testem glebokosci, wiec NIE widac ich przez sciany.
 */
public class HitboxRenderer {

    public static void render(WorldRenderContext ctx) {
        AlekConfig cfg = AlekClient.config;
        if (cfg == null || !cfg.hitboxesEnabled) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || ctx.consumers() == null || ctx.matrixStack() == null) {
            return;
        }

        MatrixStack matrices = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        float tickDelta = ctx.tickCounter().getTickDelta(false);
        VertexConsumer lines = ctx.consumers().getBuffer(RenderLayer.getLines());

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.cameraEntity && mc.options.getPerspective().isFirstPerson()) {
                continue;
            }
            if (e.squaredDistanceTo(cam) > 64.0 * 64.0) {
                continue;
            }
            float[] c = colorFor(e, cfg);
            Vec3d lerped = e.getLerpedPos(tickDelta);
            Box box = e.getBoundingBox().offset(lerped.x - e.getX(), lerped.y - e.getY(), lerped.z - e.getZ());
            drawBox(matrices, lines, box, c[0], c[1], c[2]);
        }

        matrices.pop();

        if (ctx.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
    }

    /** Wlasne rysowanie ramki (12 krawedzi), bez zaleznosci od metod Minecrafta, ktore sie przenosza. */
    private static void drawBox(MatrixStack matrices, VertexConsumer lines, Box b, float r, float g, float bl) {
        MatrixStack.Entry entry = matrices.peek();
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (bl * 255);

        // krawedzie wzdluz X
        line(entry, lines, x1, y1, z1, x2, y1, z1, ri, gi, bi, 1, 0, 0);
        line(entry, lines, x1, y2, z1, x2, y2, z1, ri, gi, bi, 1, 0, 0);
        line(entry, lines, x1, y1, z2, x2, y1, z2, ri, gi, bi, 1, 0, 0);
        line(entry, lines, x1, y2, z2, x2, y2, z2, ri, gi, bi, 1, 0, 0);
        // krawedzie wzdluz Y
        line(entry, lines, x1, y1, z1, x1, y2, z1, ri, gi, bi, 0, 1, 0);
        line(entry, lines, x2, y1, z1, x2, y2, z1, ri, gi, bi, 0, 1, 0);
        line(entry, lines, x1, y1, z2, x1, y2, z2, ri, gi, bi, 0, 1, 0);
        line(entry, lines, x2, y1, z2, x2, y2, z2, ri, gi, bi, 0, 1, 0);
        // krawedzie wzdluz Z
        line(entry, lines, x1, y1, z1, x1, y1, z2, ri, gi, bi, 0, 0, 1);
        line(entry, lines, x2, y1, z1, x2, y1, z2, ri, gi, bi, 0, 0, 1);
        line(entry, lines, x1, y2, z1, x1, y2, z2, ri, gi, bi, 0, 0, 1);
        line(entry, lines, x2, y2, z1, x2, y2, z2, ri, gi, bi, 0, 0, 1);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer lines,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int r, int g, int b, float nx, float ny, float nz) {
        lines.vertex(entry, x1, y1, z1).color(r, g, b, 255).normal(entry, nx, ny, nz);
        lines.vertex(entry, x2, y2, z2).color(r, g, b, 255).normal(entry, nx, ny, nz);
    }

    private static float[] colorFor(Entity e, AlekConfig cfg) {
        if (e instanceof PlayerEntity) {
            return cfg.playerRgb;
        }
        if (e instanceof Monster) {
            return cfg.mobRgb;
        }
        if (e instanceof AnimalEntity || e instanceof PassiveEntity
                || e instanceof WaterCreatureEntity || e instanceof AmbientEntity) {
            return cfg.animalRgb;
        }
        if (e instanceof ItemEntity) {
            return cfg.itemRgb;
        }
        return cfg.otherRgb;
    }
}
