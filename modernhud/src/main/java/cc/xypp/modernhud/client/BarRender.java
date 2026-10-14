package cc.xypp.modernhud.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Code-drawn HUD bars — the render-engine replacement for PNG bars, tuned to the Modern Warfare
 * 2022 / Warzone / DMZ look: thin, seamless RECTANGLES with an ultra-thin edge (no chunky border),
 * a faint top sheen, and armour "plates" as separated segments. Everything is {@link GuiGraphics#fill}
 * only — no textures, crisp at any scale.
 *
 * <p>Two behaviours borrowed from Damage Engine: bars glide toward their real value ({@link #smooth})
 * and are drawn in one place so measured height can't drift from what's rendered.
 */
public final class BarRender {

    private BarRender() {
    }

    /** Empty-track colour: a faint dark wash so the bar reads over any world without a hard border. */
    public static final int TRACK = 0x66000000;
    /** Ultra-thin edge line (MW "seamless" hairline). Very subtle. */
    public static final int EDGE = 0x33FFFFFF;

    // MW plate tiers, keyed by how many plates are FULLY filled. Progression per request:
    // gray (starting) → blue → purple → red. So 1 full plate is still gray, 2 = blue, 3 = purple,
    // 4+ = red — the colour only climbs as you stack more complete plates. Only used when
    // AbsorptionColorMode is TIERED; CUSTOM mode skips this entirely (see plates() below).
    public static final int TIER_GRAY = 0xFFB8BFC6;
    public static final int TIER_BLUE = 0xFF54ABE8;
    public static final int TIER_PURPLE = 0xFF9E6FE2;
    public static final int TIER_RED = 0xFFE85138;

    /** MW2022 health: clean white. */
    // Decade: health is signal red everywhere in our HUD and stays red when low (the number and the
    // breathing glow say low); upstream's MW white is 0xFFF4F6F8, its low red 0xFFE85138
    public static final int HEALTH = 0xFFE0161C;
    public static final int HEALTH_LOW = 0xFFE0161C;

    /** Decade: vanilla's heart variants carried over to the bar - poison, wither, frozen. */
    public static final int HEALTH_POISON = 0xFFA4B83A;
    public static final int HEALTH_WITHER = 0xFF6A6A72;
    public static final int HEALTH_FROZEN = 0xFF9FD8F2;

    /** Decade: the health bar's colour, in vanilla's order (HeartType.forPlayer): poison over wither over frozen, then low. */
    public static int healthColor(net.minecraft.world.entity.player.Player player, boolean low) {
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) return HEALTH_POISON;
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) return HEALTH_WITHER;
        if (player.isFullyFrozen()) return HEALTH_FROZEN;
        return low ? HEALTH_LOW : HEALTH;
    }

    /** Default "ocean blue" used when a caller wants a fixed absorption colour but supplies none. */
    public static final int OCEAN_BLUE = 0xFF1C7ED6;

    public static int tierColor(int filledSegments) {
        if (filledSegments >= 4) return TIER_RED;
        if (filledSegments == 3) return TIER_PURPLE;
        if (filledSegments == 2) return TIER_BLUE;
        return TIER_GRAY;
    }

    /** Seamless rectangular track: faint wash + a 1px top sheen line. No heavy border. */
    public static void track(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, TRACK);
        g.fill(x, y, x + w, y + 1, EDGE); // hairline top edge
    }

    /** Rectangular fill from the left, with a 1px brighter top line for the sleek MW sheen. */
    public static void fill(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        int filled = Math.round(w * clamp01(fraction));
        if (filled <= 0) return;
        g.fill(x, y, x + filled, y + h, color);
        if (h >= 3) {
            g.fill(x, y, x + filled, y + 1, lighten(color, 0.30f));
        }
    }

    /** Track + fill — the common thin rectangle bar. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        track(g, x, y, w, h);
        fill(g, x, y, w, h, fraction, color);
    }

    /**
     * MW2022 armour PLATES: {@code segments} separated rectangles with a visible gap between them.
     * Each plate fills proportionally so a half-charged plate reads correctly.
     *
     * @param useCustomColor when true, every plate uses {@code customColor} instead of the
     *                       gray/blue/purple/red tier progression (the "custom absorption colour"
     *                       config option — e.g. locking the whole bar to a single ocean blue).
     */
    public static void plates(GuiGraphics g, int x, int y, int w, int h,
                              float value, int segments, double perSegment,
                              boolean useCustomColor, int customColor) {
        if (segments <= 0) return;
        double barMax = segments * perSegment;
        int filledWhole = (int) Math.floor(Math.min(value, barMax) / perSegment);
        // useCustomColor's value comes straight from config as plain 0xRRGGBB (no alpha byte) -
        // force it opaque here so a custom absorption colour can never end up invisible.
        int color = useCustomColor ? (customColor | 0xFF000000) : tierColor(filledWhole);
        int gap = Math.max(2, w / 40);                       // clear separation between plates
        for (int i = 0; i < segments; i++) {
            // Decade: each plate's edges from the whole width, so the last one ends where the health bar
            // ends; an equal integer width left the row up to segments-1 pixels short (76 px, 4 plates)
            int cx = x + Math.round((float) i * (w + gap) / segments);
            int cellW = Math.max(1, x + Math.round((float) (i + 1) * (w + gap) / segments) - gap - cx);
            g.fill(cx, y, cx + cellW, y + h, TRACK);         // plate slot
            double segValue = value - i * perSegment;
            if (segValue > 0) {
                float frac = (float) Math.min(1.0, segValue / perSegment);
                int fw = Math.round(cellW * frac);
                g.fill(cx, y, cx + fw, y + h, color);
                if (h >= 3) g.fill(cx, y, cx + fw, y + 1, lighten(color, 0.30f));
            }
        }
    }

    /** How much absorption is hidden above what the bar can visually represent (never negative). */
    public static float overflow(float value, int segments, double perSegment) {
        double barMax = segments * perSegment;
        return (float) Math.max(0.0, value - barMax);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static int lighten(int argb, float amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int) (r + (255 - r) * amount);
        g = (int) (g + (255 - g) * amount);
        b = (int) (b + (255 - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
