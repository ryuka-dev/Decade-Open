package dev.xylonity.tooltipoverhaul.client.decade;

import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Decade: what the Decade server's client mod (decade_hud) tells this fork, by Forge inter-mod messages in plain Java and
 * Minecraft types only (decade_hud is not GPL and does not link this mod; see README-DECADE.md). Without it every hook
 * answers as upstream would: no handbook entry, never typing, nothing to open.
 *
 * <ul>
 *   <li>{@code entry}: the handbook entry a tooltip belongs to, for its stack (an empty stack: the text tooltip being
 *       drawn now), or null</li>
 *   <li>{@code typing}: whether one of decade_hud's own text fields has the focus (they are not vanilla EditBoxes)</li>
 *   <li>{@code open}: open the handbook at an entry</li>
 * </ul>
 */
public final class DecadeHooks {
    public static volatile Function<ItemStack, String> entry;
    public static volatile BooleanSupplier typing;
    public static volatile Consumer<String> open;
    /** The handbook's nouns in a line of text: "start,end,entry" each, in the line's characters; none without hud. */
    public static volatile Function<String, java.util.List<String>> nouns;
    /** The colour of a stack's grade (civilian, military) for its icon box as 0xRRGGBB, or 0 for none; none without hud. */
    public static volatile Function<ItemStack, Integer> grade;

    private DecadeHooks() {
    }

    public static String entry(ItemStack stack) {
        Function<ItemStack, String> f = entry;
        try {
            return f == null ? null : f.apply(stack == null ? ItemStack.EMPTY : stack);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static boolean typing() {
        BooleanSupplier t = typing;
        try {
            return t != null && t.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static java.util.List<String> nouns(String line) {
        Function<String, java.util.List<String>> f = nouns;
        try {
            java.util.List<String> out = f == null ? null : f.apply(line);
            return out == null ? java.util.List.of() : out;
        } catch (RuntimeException e) {
            return java.util.List.of();
        }
    }

    public static int grade(ItemStack stack) {
        Function<ItemStack, Integer> f = grade;
        try {
            Integer c = f == null || stack == null || stack.isEmpty() ? null : f.apply(stack);
            return c == null ? 0 : c & 0xFFFFFF;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public static boolean open(String id) {
        Consumer<String> o = open;
        if (o == null || id == null) {
            return false;
        }
        o.accept(id);
        return true;
    }
}
