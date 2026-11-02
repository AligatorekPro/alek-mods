package pl.alek.alekclient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Slup swiatla nad wyrzuconymi przedmiotami. Kolor slupa = kolor, ktorego jest najwiecej w teksturze przedmiotu
 * (czyta teksture z aktualnej paczki zasobow, wiec pasuje tez do twojej paczki tekstur).
 */
public class LootBeams {
    private static final Map<Item, Integer> COLORS = new HashMap<>();

    public static void render(WorldRenderContext ctx) {
        AlekConfig cfg = AlekClient.config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || !cfg.lootBeams || mc.world == null || ctx.consumers() == null || ctx.matrixStack() == null) {
            return;
        }

        MatrixStack matrices = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        float tickDelta = ctx.tickCounter().getTickDelta(false);
        VertexConsumer buf = ctx.consumers().getBuffer(RenderLayer.getLightning());

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity item)) {
                continue;
            }
            if (e.squaredDistanceTo(cam) > 48.0 * 48.0) {
                continue;
            }
            int rgb = COLORS.computeIfAbsent(item.getStack().getItem(), LootBeams::computeColor);
            drawBeam(mat, buf, item.getLerpedPos(tickDelta), rgb);
        }

        matrices.pop();
        if (ctx.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLightning());
        }
    }

    /** Dwie przecinajace sie plaszczyzny: u dolu jasne, ku gorze zanikaja. */
    private static void drawBeam(Matrix4f mat, VertexConsumer buf, Vec3d p, int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        float w = 0.08f;
        float y0 = (float) p.y + 0.05f;
        float y1 = y0 + 2.5f;
        float x = (float) p.x;
        float z = (float) p.z;
        int aBottom = 170;
        int aTop = 0;

        // plaszczyzna wzdluz X
        buf.vertex(mat, x - w, y0, z).color(r, g, b, aBottom);
        buf.vertex(mat, x + w, y0, z).color(r, g, b, aBottom);
        buf.vertex(mat, x + w, y1, z).color(r, g, b, aTop);
        buf.vertex(mat, x - w, y1, z).color(r, g, b, aTop);
        // plaszczyzna wzdluz Z
        buf.vertex(mat, x, y0, z - w).color(r, g, b, aBottom);
        buf.vertex(mat, x, y0, z + w).color(r, g, b, aBottom);
        buf.vertex(mat, x, y1, z + w).color(r, g, b, aTop);
        buf.vertex(mat, x, y1, z - w).color(r, g, b, aTop);
    }

    private static int computeColor(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        String ns = id.getNamespace();
        String path = id.getPath();
        String[] candidates = {
                "textures/item/" + path + ".png",
                "textures/block/" + path + ".png",
                "textures/block/" + path + "_top.png",
                "textures/block/" + path + "_front.png",
                "textures/block/" + path + "_side.png"
        };
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        for (String candidate : candidates) {
            Optional<Resource> res = rm.getResource(Identifier.of(ns, candidate));
            if (res.isPresent()) {
                try (InputStream in = res.get().getInputStream()) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) {
                        int c = dominant(img, 30);
                        if (c < 0) {
                            c = dominant(img, 0);
                        }
                        if (c >= 0) {
                            return c;
                        }
                    }
                } catch (Exception ignored) {
                    // sprobuj nastepna tekstura
                }
            }
        }
        return 0xFFFFFF;
    }

    /** Najczesciej wystepujacy kolor (po pogrupowaniu zblizonych odcieni). Ciemne obrysy sa pomijane. */
    private static int dominant(BufferedImage img, int minBrightness) {
        int w = img.getWidth();
        int h = Math.min(img.getHeight(), w); // animowane tekstury: tylko pierwsza klatka
        Map<Integer, int[]> buckets = new HashMap<>(); // klucz -> {liczba, sumaR, sumaG, sumaB}
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = argb >>> 24;
                if (a < 128) {
                    continue;
                }
                int r = (argb >> 16) & 255;
                int g = (argb >> 8) & 255;
                int b = argb & 255;
                if (Math.max(r, Math.max(g, b)) < minBrightness) {
                    continue;
                }
                int key = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);
                int[] bucket = buckets.computeIfAbsent(key, k -> new int[4]);
                bucket[0]++;
                bucket[1] += r;
                bucket[2] += g;
                bucket[3] += b;
            }
        }
        int[] best = null;
        for (int[] bucket : buckets.values()) {
            if (best == null || bucket[0] > best[0]) {
                best = bucket;
            }
        }
        if (best == null) {
            return -1;
        }
        return ((best[1] / best[0]) << 16) | ((best[2] / best[0]) << 8) | (best[3] / best[0]);
    }
}
