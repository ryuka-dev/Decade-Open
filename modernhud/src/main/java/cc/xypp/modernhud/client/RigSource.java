package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Decade: a charge row over the bottom-left panel, for a worn device another mod supplies. Handed over as an inter-mod
 * message so the supplying mod never links against this one: one message {@code rig} to {@code modernhud}, carrying a
 * supplier of a plain {@code Supplier<Object[]>} that returns, each frame, {@code {charge, full, readyAtMillis, key,
 * icon, glowRgb}} (two Floats, a Long, a String, an ItemStack, an Integer), or {@code null} with nothing worn.
 * <pre>
 *   [icon]  [#][#][#][#][#][#][ ][ ][ ][ ]  60%      charging: cream cells and a percentage
 *   [icon]  [#][#][#][#][#][#][#][#][#][#]  [G]      full: the cells in its colour, a key cap; one flash as it fills
 * </pre>
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RigSource {
    public static final String MESSAGE = "rig";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CELLS = 10;
    private static final int CREAM = 0xFFE8E2D6;
    /** The row's height, and the space between it and the panel under it. */
    static final int ROW_H = 3;
    static final int GAP = 6;

    private static volatile Supplier<Object[]> source;
    /** How long the shown charge takes to close most of the way to a new value: a gain climbs, it does not jump. */
    private static final float EASE_MS = 400f;
    /** The charge as drawn, climbing after the real one; -1 before the first frame. Render thread only. */
    private static float shown = -1;
    private static float shownFull = -1;
    private static long lastFrame;

    private RigSource() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onInterModProcess(InterModProcessEvent event) {
        InterModComms.getMessages(ModernHud.MOD_ID, MESSAGE::equals).forEach(message -> {
            Object payload = message.messageSupplier().get();
            if (payload instanceof Supplier<?> s) {
                source = (Supplier<Object[]>) s;
                LOGGER.info("Device source registered by {}", message.senderModId());
            } else {
                LOGGER.error("{} from {} is not a Supplier, ignored", MESSAGE, message.senderModId());
            }
        });
    }

    /**
     * The charge to draw: climbing toward {@code real} so every gain shows as a short rise; down at once (it was spent,
     * or the device changed), and full at once, so the ready flash and the full bar come together.
     */
    static float ease(float real, float full) {
        long now = System.currentTimeMillis();
        float dt = shown < 0 ? 0 : Math.min(250, now - lastFrame);
        lastFrame = now;
        if (shown < 0 || full != shownFull || real < shown || real >= full) {
            shown = real;
            shownFull = full;
            return shown;
        }
        shown += (real - shown) * (1 - (float) Math.exp(-dt / EASE_MS));
        if (real - shown < 0.05f) {
            shown = real;
        }
        return shown;
    }

    /** This frame's row, or null. */
    static Object[] read() {
        Supplier<Object[]> s = source;
        if (s == null) {
            return null;
        }
        try {
            Object[] v = s.get();
            if (v == null || v.length < 6 || !(v[0] instanceof Float) || !(v[1] instanceof Float) || !((Float) v[1] > 0)
                    || !(v[2] instanceof Long) || !(v[3] instanceof String) || !(v[4] instanceof ItemStack)
                    || !(v[5] instanceof Integer)) {
                return null;
            }
            return v;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Draws the row with its bottom at {@code bottom}: the icon in the head column, the cells as wide as the bars,
     * the percentage or the key cap where the HP number stands.
     */
    static void draw(GuiGraphics g, Font font, Object[] v, int barX, int barWidth, int numberX, int bottom) {
        float charge = ease((Float) v[0], (Float) v[1]), full = (Float) v[1];
        long readyAt = (Long) v[2];
        String key = (String) v[3];
        ItemStack icon = (ItemStack) v[4];
        int glow = (Integer) v[5] | 0xFF000000;
        boolean ready = charge >= full;
        int y = bottom - ROW_H;
        if (!icon.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(Math.max(0, barX - 14), y - 4, 0);
            g.pose().scale(0.6875f, 0.6875f, 1);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();
        }
        float filled = Math.max(0, Math.min(1, charge / full)) * CELLS;
        int color = ready ? glow : CREAM;
        for (int i = 0; i < CELLS; i++) {
            int x0 = barX + Math.round(i * (barWidth + 1) / (float) CELLS);
            int x1 = barX + Math.round((i + 1) * (barWidth + 1) / (float) CELLS) - 1;
            g.fill(x0, y, x1, y + ROW_H, BarRender.TRACK);
            float part = Math.max(0, Math.min(1, filled - i));
            if (part > 0) {
                int w = Math.max(1, Math.round((x1 - x0) * part));
                g.fill(x0, y, x0 + w, y + ROW_H, color);
                g.fill(x0, y, x0 + w, y + 1, BarRender.lighten(color, 0.30f));
            }
        }
        long since = System.currentTimeMillis() - readyAt;
        if (ready) {
            float flash = since >= 0 && since < 700 ? (1 - since / 700f) * (1 - since / 700f) : 0;
            float breathe = 0.18f + 0.12f * (float) Math.sin(System.currentTimeMillis() / 420.0);
            GlowEffect.drawGlowRect(g, barX, y, barWidth, ROW_H, glow & 0xFFFFFF, Math.max(flash, breathe), 3f);
            if (flash > 0.01f) {
                g.fill(barX, y, barX + barWidth, y + ROW_H, Math.round(flash * 0xC0) << 24 | 0xFFFFFF);
            }
            // a key cap: the key that fires it
            int kw = font.width(key) + 4, ky = bottom - 9;
            g.fill(numberX, ky, numberX + kw, ky + 10, 0xC0E8ECF0);
            g.fill(numberX + 1, ky + 1, numberX + kw - 1, ky + 9, 0xD90A0D11);
            g.drawString(font, key, numberX + 2, ky + 1, 0xFFFFFFFF, false);
        } else {
            String pct = Math.round(charge * 100 / full) + "%";
            g.drawString(font, pct, numberX, bottom - 7, 0xB3E8E2D6, true);
        }
    }
}
