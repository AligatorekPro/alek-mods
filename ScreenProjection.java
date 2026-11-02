package pl.alek.alekclient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/** Zamienia punkt w swiecie na pozycje na ekranie (0..1). Tworzone raz na klatke, w czasie renderowania swiata. */
public class ScreenProjection {
    private final Matrix4f viewProjection;
    private final Vec3d cam;

    public ScreenProjection(WorldRenderContext ctx) {
        this.cam = ctx.camera().getPos();
        Matrix4f view = new Matrix4f().rotation(ctx.camera().getRotation().conjugate(new Quaternionf()));
        this.viewProjection = new Matrix4f(ctx.projectionMatrix()).mul(view);
    }

    /** Zwraca {x, y} w zakresie 0..1 albo null, jesli punkt jest za plecami lub poza ekranem. */
    public float[] project(Vec3d worldPos) {
        Vec3d rel = worldPos.subtract(cam);
        Vector4f v = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1.0f);
        viewProjection.transform(v);
        if (v.w <= 0.1f) {
            return null;
        }
        float nx = v.x / v.w;
        float ny = v.y / v.w;
        if (Math.abs(nx) > 1.1f || Math.abs(ny) > 1.1f) {
            return null;
        }
        return new float[]{nx * 0.5f + 0.5f, 0.5f - ny * 0.5f};
    }
}
