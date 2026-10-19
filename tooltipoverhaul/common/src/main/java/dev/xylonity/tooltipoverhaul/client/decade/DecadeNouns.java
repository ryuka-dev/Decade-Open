package dev.xylonity.tooltipoverhaul.client.decade;

import dev.xylonity.tooltipoverhaul.mixin.ClientTextTooltipAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Decade: the handbook's nouns in a pinned tooltip: under every noun a dotted line;
 * the noun under the mouse turns white and its line solid; a click on it opens that noun's entry rather than the
 * tooltip's own. Which spans are nouns is decade_hud's ({@link DecadeHooks#nouns}); this only finds where each line is
 * drawn. Drawn while a pin is drawn ({@link #begin}, {@link #end}), so each pin keeps its own noun boxes. The title is
 * underlined whole when the thing itself has an entry (the owner, 2026-10-19).
 */
public final class DecadeNouns {
    /** A noun's box in the pin's own coordinates and the entry it opens. */
    public record Box(float x0, float y0, float x1, float y1, String entry) {
        public boolean contains(double mx, double my) {
            return mx >= x0 && mx < x1 && my >= y0 && my < y1;
        }
    }

    private static List<Box> current;
    private static String titleEntry;
    private static double mouseX, mouseY;
    private static String jumpableKey;
    private static boolean jumpableLast;

    private DecadeNouns() {
    }

    /** The mouse in the pin's own coordinates (a screen may draw its tooltips scaled), and the pin's own entry. */
    public static void begin(double mx, double my, String entry) {
        current = new ArrayList<>();
        titleEntry = entry;
        mouseX = mx;
        mouseY = my;
    }

    public static List<Box> end() {
        List<Box> out = current == null ? List.of() : List.copyOf(current);
        current = null;
        titleEntry = null;
        return out;
    }

    public static String plain(ClientTooltipComponent component) {
        StringBuilder out = new StringBuilder();
        if (component instanceof ClientTextTooltip text) {
            ((ClientTextTooltipAccessor) text).getText().accept((index, style, codePoint) -> {
                out.appendCodePoint(codePoint);
                return true;
            });
        }
        return out.toString();
    }

    /**
     * Whether a tooltip has anything to open: its own entry, or a noun in a line below the title (the owner, 2026-10-19:
     * one with nothing to open cannot be pinned). The last answer is kept while the lines stay the same.
     */
    public static boolean jumpable(String entry, List<ClientTooltipComponent> components) {
        if (entry != null) {
            return true;
        }
        StringBuilder key = new StringBuilder();
        List<String> lines = new ArrayList<>();
        int start = !components.isEmpty() && components.get(0) instanceof ClientTextTooltip ? 1 : 0;
        for (int i = start; i < components.size(); i++) {
            String line = plain(components.get(i));
            lines.add(line);
            key.append(line).append('\n');
        }
        String k = key.toString();
        if (!k.equals(jumpableKey)) {
            boolean any = false;
            for (String line : lines) {
                if (!line.isEmpty() && !DecadeHooks.nouns(line).isEmpty()) {
                    any = true;
                    break;
                }
            }
            jumpableKey = k;
            jumpableLast = any;
        }
        return jumpableLast;
    }

    /** After a pin's title is drawn at (x, y): underlined whole when the thing has an entry of its own. */
    public static void title(GuiGraphics graphics, Font font, ClientTooltipComponent component, int x, int y) {
        if (current == null || titleEntry == null || !(component instanceof ClientTextTooltip)) {
            return;
        }
        String s = plain(component);
        if (s.isBlank()) {
            return;
        }
        underline(graphics, font, s, x, y, x, x + font.width(s), titleEntry);
    }

    /** After a line is drawn at (x, y) in a pinned tooltip: its nouns underlined and remembered. */
    public static void line(GuiGraphics graphics, Font font, ClientTooltipComponent component, int x, int y) {
        if (current == null || !(component instanceof ClientTextTooltip)) {
            return;
        }
        String s = plain(component);
        List<String> spans = DecadeHooks.nouns(s);
        for (String span : spans) {
            String[] p = span.split(",", 3);
            if (p.length < 3) {
                continue;
            }
            int start, end;
            try {
                start = Integer.parseInt(p[0]);
                end = Integer.parseInt(p[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (start < 0 || end > s.length() || end <= start) {
                continue;
            }
            underline(graphics, font, s.substring(start, end), x0(font, s, x, start), y, x0(font, s, x, start),
                    x0(font, s, x, end), p[2]);
        }
    }

    private static int x0(Font font, String s, int x, int index) {
        return x + font.width(s.substring(0, index));
    }

    private static void underline(GuiGraphics graphics, Font font, String word, int textX, int y, int x0, int x1, String entry) {
        Box box = new Box(x0, y - 1, x1, y + 10, entry);
        current.add(box);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 50);
        if (box.contains(mouseX, mouseY)) {
            graphics.drawString(font, word, textX, y, 0xFFFFFFFF, true);
            graphics.fill(x0, y + 9, x1, y + 10, 0xFFFFFFFF);
        } else {
            for (int dx = x0; dx < x1; dx += 2) {
                graphics.fill(dx, y + 9, Math.min(dx + 1, x1), y + 10, 0xC0D0D0D0);
            }
        }
        graphics.pose().popPose();
    }
}
