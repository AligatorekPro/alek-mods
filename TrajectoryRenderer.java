package pl.alek.alekclient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Trajektorie pociskow: twoje (luk, kusza, trojzab, perla, sniezka, jajko, mikstury, butelka expa)
 * oraz cudze pociski w locie. Pokazuje tez miejsce upadku i odliczanie czasu lotu.
 */
public class TrajectoryRenderer {

    private record Label(String text, float nx, float ny, int color) {
    }

    private record Sim(List<Vec3d> points, BlockHitResult hit, int ticks) {
    }

    private static List<Label> labels = new ArrayList<>();

    public static void clear() {
        labels = new ArrayList<>();
    }

    public static void render(WorldRenderContext ctx) {
        AlekConfig cfg = AlekClient.config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || !cfg.trajectories || mc.world == null || mc.player == null
                || ctx.consumers() == null || ctx.matrixStack() == null) {
            labels = new ArrayList<>();
            return;
        }

        float tickDelta = ctx.tickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        ScreenProjection projection = new ScreenProjection(ctx);
        List<Label> newLabels = new ArrayList<>();

        MatrixStack matrices = ctx.matrixStack();
        VertexConsumer lines = ctx.consumers().getBuffer(RenderLayer.getLines());
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        Sim own = simulateOwn(mc, tickDelta);
        if (own != null) {
            drawSim(matrices, lines, own, projection, newLabels, 1.0f, 0.41f, 0.71f);
        }

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ProjectileEntity) || e.squaredDistanceTo(cam) > 96.0 * 96.0) {
                continue;
            }
            double gravity = gravityFor(e);
            if (Double.isNaN(gravity)) {
                continue;
            }
            Vec3d vel = e.getVelocity();
            if (vel.lengthSquared() < 0.0025) {
                continue; // lezy w ziemi
            }
            Sim sim = simulate(mc, e.getLerpedPos(tickDelta), vel, gravity, e);
            drawSim(matrices, lines, sim, projection, newLabels, 0.4f, 0.8f, 1.0f);
        }

        matrices.pop();
        if (ctx.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
        labels = newLabels;
    }

    /** Odliczanie czasu lotu przy miejscu upadku. */
    public static void draw(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (labels.isEmpty() || mc.options.hudHidden || mc.currentScreen != null) {
            return;
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        for (Label l : labels) {
            ctx.drawCenteredTextWithShadow(mc.textRenderer, l.text(), (int) (l.nx() * w), (int) (l.ny() * h), l.color());
        }
    }

    private static double gravityFor(Entity e) {
        if (e instanceof PersistentProjectileEntity) {
            return 0.05; // strzaly, trojzab
        }
        if (e instanceof ExperienceBottleEntity) {
            return 0.07;
        }
        if (e instanceof ThrownEntity) {
            return 0.03; // perla, sniezka, jajko (mikstury leca prawie tak samo)
        }
        return Double.NaN;
    }

    private static Sim simulateOwn(MinecraftClient mc, float tickDelta) {
        PlayerEntity p = mc.player;
        for (Hand hand : Hand.values()) {
            ItemStack stack = p.getStackInHand(hand);
            double speed;
            double gravity;
            float roll = 0.0f;

            if (stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.SNOWBALL) || stack.isOf(Items.EGG)) {
                speed = 1.5;
                gravity = 0.03;
            } else if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
                speed = 0.5;
                gravity = 0.05;
                roll = -20.0f;
            } else if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
                speed = 0.7;
                gravity = 0.07;
                roll = -20.0f;
            } else if (stack.isOf(Items.TRIDENT) && p.isUsingItem() && p.getActiveItem().isOf(Items.TRIDENT)) {
                speed = 2.5;
                gravity = 0.05;
            } else if (stack.isOf(Items.BOW) && p.isUsingItem() && p.getActiveItem().isOf(Items.BOW)) {
                float pull = BowItem.getPullProgress(p.getItemUseTime());
                if (pull < 0.1f) {
                    continue;
                }
                speed = pull * 3.0;
                gravity = 0.05;
            } else if (stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
                speed = 3.15;
                gravity = 0.05;
            } else {
                continue;
            }

            double yaw = Math.toRadians(p.getYaw(tickDelta));
            double pitch = Math.toRadians(p.getPitch(tickDelta));
            double pitchRoll = Math.toRadians(p.getPitch(tickDelta) + roll);
            Vec3d dir = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitchRoll), Math.cos(yaw) * Math.cos(pitch));
            Vec3d vel = dir.multiply(speed);
            Vec3d pv = p.getVelocity();
            vel = vel.add(pv.x, p.isOnGround() ? 0.0 : pv.y, pv.z);
            Vec3d start = p.getCameraPosVec(tickDelta).subtract(0.0, 0.1, 0.0);
            return simulate(mc, start, vel, gravity, p);
        }
        return null;
    }

    private static Sim simulate(MinecraftClient mc, Vec3d start, Vec3d velocity, double gravity, Entity source) {
        List<Vec3d> points = new ArrayList<>();
        points.add(start);
        Vec3d pos = start;
        Vec3d vel = velocity;
        BlockHitResult hit = null;
        int ticks = 0;
        for (; ticks < 300; ticks++) {
            Vec3d next = pos.add(vel);
            BlockHitResult r = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
            if (r.getType() != HitResult.Type.MISS) {
                points.add(r.getPos());
                hit = r;
                ticks++;
                break;
            }
            points.add(next);
            pos = next;
            vel = vel.multiply(0.99).subtract(0.0, gravity, 0.0);
            if (pos.y < mc.world.getBottomY() - 10) {
                break;
            }
        }
        return new Sim(points, hit, ticks);
    }

    private static void drawSim(MatrixStack matrices, VertexConsumer lines, Sim sim, ScreenProjection projection,
                                List<Label> out, float r, float g, float b) {
        MatrixStack.Entry entry = matrices.peek();
        int ri = (int) (r * 255);
        int gi = (int) (g * 255);
        int bi = (int) (b * 255);

        List<Vec3d> pts = sim.points();
        for (int i = 1; i < pts.size(); i++) {
            Vec3d a = pts.get(i - 1);
            Vec3d c = pts.get(i);
            Vec3d d = c.subtract(a);
            double len = d.length();
            if (len < 1.0e-6) {
                continue;
            }
            d = d.multiply(1.0 / len);
            HitboxRenderer.line(entry, lines,
                    (float) a.x, (float) a.y, (float) a.z, (float) c.x, (float) c.y, (float) c.z,
                    ri, gi, bi, (float) d.x, (float) d.y, (float) d.z);
        }

        if (sim.hit() != null) {
            // podswietlenie bloku, w ktory trafi
            HitboxRenderer.drawBox(matrices, lines, new Box(sim.hit().getBlockPos()).expand(0.002), r, g, b);
            Vec3d p = sim.hit().getPos();
            HitboxRenderer.drawBox(matrices, lines,
                    new Box(p.x - 0.12, p.y - 0.12, p.z - 0.12, p.x + 0.12, p.y + 0.12, p.z + 0.12), 1.0f, 1.0f, 1.0f);
            float[] s = projection.project(p.add(0.0, 0.6, 0.0));
            if (s != null) {
                out.add(new Label(String.format("%.1fs", sim.ticks() / 20.0), s[0], s[1],
                        0xFF000000 | (ri << 16) | (gi << 8) | bi));
            }
        }
    }
}
