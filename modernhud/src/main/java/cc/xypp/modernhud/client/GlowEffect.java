package cc.xypp.modernhud.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A soft "glow halo" behind a flat-coloured rectangle: several offset copies of the same rect,
 * drawn with additive blending at decreasing alpha further from centre, giving a cheap glow/bloom
 * look without an actual shader pass. Same layering technique as a reference kill-feed mod's
 * icon-glow effect (studied for this v3.0 rewrite), adapted here for flat rects (health bar, ammo
 * box) rather than sprite icons, and reimplemented from scratch in this project's own style.
 *
 * <p>Always additive-blended ({@code SRC_ALPHA, ONE}) regardless of the caller's current blend
 * state, and always restored to the default blend function afterward so it never leaks into
 * whatever renders next.
 */
public final class GlowEffect {

    private static final float[][] OFFSETS = {
            {-1f, -1f}, {0f, -1f}, {1f, -1f},
            {-1f, 0f}, {1f, 0f},
            {-1f, 1f}, {0f, 1f}, {1f, 1f},
    };

    private GlowEffect() {
    }

    /**
     * Draws a glow halo around a {@code w x h} rect at {@code (x, y)}.
     *
     * @param colorRgb glow colour, 0xRRGGBB
     * @param alpha    overall glow strength, 0-1 — already including any fade/pulse the caller wants
     * @param spread   how far (px) the outer ring of copies sits from the rect's edge
     */
    public static void drawGlowRect(GuiGraphics g, int x, int y, int w, int h, int colorRgb, float alpha, float spread) {
        if (alpha <= 0.001f || spread <= 0.01f || w <= 0 || h <= 0) {
            return;
        }
        float outer = Math.max(0.5f, spread);
        float inner = outer * 0.55f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        for (float[] off : OFFSETS) {
            drawRect(g, x + Math.round(off[0] * outer), y + Math.round(off[1] * outer), w, h, colorRgb, alpha * 0.16f);
        }
        for (float[] off : OFFSETS) {
            drawRect(g, x + Math.round(off[0] * inner), y + Math.round(off[1] * inner), w, h, colorRgb, alpha * 0.11f);
        }
        drawRect(g, x, y, w, h, colorRgb, alpha * 0.09f);

        RenderSystem.defaultBlendFunc();
    }

    private static void drawRect(GuiGraphics g, int x, int y, int w, int h, int colorRgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        if (a <= 0) {
            return;
        }
        g.fill(x, y, x + w, y + h, (a << 24) | (colorRgb & 0xFFFFFF));
    }
}
