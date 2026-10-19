package dev.xylonity.tooltipoverhaul.client.decade;

import dev.xylonity.tooltipoverhaul.client.render.TooltipContext;
import dev.xylonity.tooltipoverhaul.registry.TooltipOverhaulKeyMappings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Decade: the corner book: a tooltip that has a handbook entry shows a small book
 * inside its top-right corner, pinned or not; one without an entry shows none. A hovered tooltip that can be pinned (it
 * has something to open, {@link DecadeNouns#jumpable}) also says 「按 F 以固定」 on the book's row, right up against its
 * left (the owner, 2026-10-19). The tooltip is made wider for both.
 */
public final class CornerBook {
    private static final ItemStack BOOK = new ItemStack(Items.WRITABLE_BOOK);
    /** The book's side (an item drawn at half size). */
    private static final int BOOK_SIZE = 8;
    /** Between the title and what stands in the corner. */
    private static final int GAP = 4;
    /** Between the hint and the book. */
    private static final int HINT_GAP = 2;
    private static final int HINT_COLOUR = 0xFFA0A0A0;
    /** The entry of the pin being drawn now (PinnedTooltipState sets it around its drawing), or null. */
    public static String pinEntry;

    private CornerBook() {
    }

    private static boolean book(TooltipContext context) {
        // pinned too: a pin looks like the tooltip it was (the owner, 2026-10-19); a pin knows its own entry
        if (context.isPinned()) {
            return pinEntry != null;
        }
        return context.isMainTooltip() && DecadeHooks.entry(context.getStack()) != null;
    }

    /** The hint for a hovered tooltip that can be pinned, or null. */
    private static String hint(TooltipContext context) {
        if (!context.isMainTooltip() || context.isPinned() || TooltipOverhaulKeyMappings.PIN_TOOLTIP.isUnbound()) {
            return null;
        }
        if (!DecadeNouns.jumpable(DecadeHooks.entry(context.getStack()), context.getComponents())) {
            return null;
        }
        return "按 " + TooltipOverhaulKeyMappings.PIN_TOOLTIP.getTranslatedKeyMessage().getString() + " 以固定";
    }

    public static int room(TooltipContext context) {
        boolean book = book(context);
        String hint = hint(context);
        if (!book && hint == null) {
            return 0;
        }
        int room = GAP + (book ? BOOK_SIZE : 0);
        if (hint != null) {
            room += context.getFont().width(hint) + (book ? HINT_GAP : 0);
        }
        return room;
    }

    public static void draw(TooltipContext context) {
        if (context.getTooltipPosition() == null || context.getTooltipSize() == null) {
            return;
        }
        boolean book = book(context);
        String hint = hint(context);
        if (!book && hint == null) {
            return;
        }
        float right = context.getTooltipPosition().x + context.getTooltipSize().x - context.getPaddingX();
        float y = context.getTooltipPosition().y + context.getPaddingY() - 1;
        var graphics = context.getGraphics();
        var pose = graphics.pose();
        // over every layer of the tooltip (they go up to LayerDepth.EFFECT, 2300; at 400 the background covered it, seen
        // in game 2026-10-19)
        graphics.flush();
        float z = dev.xylonity.tooltipoverhaul.client.layer.LayerDepth.EFFECT.getZ() + 100;
        if (book) {
            pose.pushPose();
            pose.translate(right - BOOK_SIZE, y, z);
            pose.scale(0.5f, 0.5f, 1);
            graphics.renderItem(BOOK, 0, 0);
            pose.popPose();
        }
        if (hint != null) {
            // the font's Chinese glyphs are 8 high, as high as the book
            float hintRight = book ? right - BOOK_SIZE - HINT_GAP : right;
            pose.pushPose();
            pose.translate(0, 0, z);
            graphics.drawString(context.getFont(), hint, (int) hintRight - context.getFont().width(hint), (int) y, HINT_COLOUR, true);
            pose.popPose();
        }
        graphics.flush();
    }
}
