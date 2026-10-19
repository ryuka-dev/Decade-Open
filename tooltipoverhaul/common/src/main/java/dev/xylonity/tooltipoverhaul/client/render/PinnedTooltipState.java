package dev.xylonity.tooltipoverhaul.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.xylonity.tooltipoverhaul.TooltipOverhaul;
import dev.xylonity.tooltipoverhaul.client.decade.DecadeHooks;
import dev.xylonity.tooltipoverhaul.client.layout.TooltipLayout;
import dev.xylonity.tooltipoverhaul.client.screen.config.EffectEditorScreen;
import dev.xylonity.tooltipoverhaul.client.screen.config.FrameEditorScreen;
import dev.xylonity.tooltipoverhaul.client.screen.config.TooltipOverhaulConfigScreen;
import dev.xylonity.tooltipoverhaul.client.style.animation.TooltipAnimator;
import dev.xylonity.tooltipoverhaul.client.util.Constants;
import dev.xylonity.tooltipoverhaul.client.util.TextUtils;
import dev.xylonity.tooltipoverhaul.client.util.TooltipScrollState;
import dev.xylonity.tooltipoverhaul.compat.proxy.ScreenTypeProxy;
import dev.xylonity.tooltipoverhaul.config.TooltipsConfig;
import dev.xylonity.tooltipoverhaul.registry.TooltipOverhaulKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Pinned item tooltips that survive closing and reopening inventory screens
 */
public final class PinnedTooltipState {

    private static final int BADGE_HEIGHT = 16;

    private static Screen owner;

    private static ItemStack hovered = ItemStack.EMPTY;
    private static List<ClientTooltipComponent> hoveredComponents = List.of();
    // Decade: the handbook entry of the tooltip last hovered, asked when it was captured
    private static String hoveredEntry;
    // Decade: a press on a pin's body, a click if released where it was pressed, a drag once it moves
    private static Pin pressed;
    private static double pressX, pressY;
    // Decade: the hovered tooltip's place, so a pin stays exactly where it was (the owner, 2026-10-19: "as if the tooltip
    // under the mouse were fixed in place"), and that tooltip, hidden once it is pinned until the mouse leaves it
    private static Vec2 hoveredAt;
    // Decade: how the screen drew it (the market board draws its tooltips scaled down), and the mouse x it was laid out for
    private static float hoveredSx = 1, hoveredSy = 1, hoveredTx, hoveredTy;
    private static int hoveredMouseX;
    private static ItemStack suppressStack;
    private static String suppressLine;
    private static long suppressSeen;

    private static final List<Pin> pins = new ArrayList<>();

    private static long lastHover;
    private static Player pinnedPlayer;
    private static Pin dragging;
    private static int consumedButton = -1;
    private static double dragX, dragY;

    private static boolean allowed(Screen screen) {
        // Decade: every screen, not only container-like ones (Decade's own screens extend Screen), but this mod's own editors
        final boolean inventoryLike = screen != null;
        return Minecraft.getInstance().player != null && inventoryLike && !(screen instanceof FrameEditorScreen)
                && !(screen instanceof EffectEditorScreen) && !(screen instanceof TooltipOverhaulConfigScreen);
    }

    private static void attach(Screen screen) {
        if (owner == screen) {
            return;
        }

        owner = screen;
        hovered = ItemStack.EMPTY;
        hoveredComponents = List.of();
        hoveredEntry = null;
        lastHover = 0;
        dragging = null;
        pressed = null;
        consumedButton = -1;
    }

    public static void capture(TooltipContext context) {
        final Screen screen = Minecraft.getInstance().screen;
        // Decade: a text tooltip (no stack) can be pinned too
        if (!allowed(screen) || context.getComponents().isEmpty()) {
            return;
        }

        attach(screen);
        hovered = context.getStack();
        hoveredComponents = context.getComponents();
        hoveredEntry = DecadeHooks.entry(hovered);
        hoveredAt = context.getTooltipPosition();
        final org.joml.Matrix4f m = context.getGraphics().pose().last().pose();
        final boolean usable = m.m00() > 0.05f && m.m11() > 0.05f;
        hoveredSx = usable ? m.m00() : 1;
        hoveredSy = usable ? m.m11() : 1;
        hoveredTx = usable ? m.m30() : 0;
        hoveredTy = usable ? m.m31() : 0;
        hoveredMouseX = context.getMouseX();
        lastHover = System.nanoTime();
    }

    public static void clear() {
        owner = null;
        hovered = ItemStack.EMPTY;
        hoveredComponents = List.of();
        pins.clear();
        lastHover = 0;
        pinnedPlayer = null;
        dragging = null;
        consumedButton = -1;
    }

    /**
     * Changes input ownership without discarding the pinned items
     */
    public static void screenChanged(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            clear();
            return;
        }

        attach(allowed(screen) ? screen : null);
    }

    private static void unpin(Pin pin) {
        pins.remove(pin);
        if (dragging == pin) {
            dragging = null;
        }

        if (pins.isEmpty()) {
            pinnedPlayer = null;
        }

    }

    private static void pinHovered() {
        final Minecraft client = Minecraft.getInstance();
        while (pins.size() >= Math.max(1, TooltipsConfig.MAX_PINNED_TOOLTIPS)) {
            unpin(pins.get(0));
        }

        pinnedPlayer = client.player;
        // Decade: in the hovered tooltip's place, and that tooltip put away (its "out" animation too)
        final float px = hoveredAt != null ? hoveredAt.x : 24 + pins.size() * 16;
        final float py = hoveredAt != null ? hoveredAt.y : 40 + pins.size() * 16;
        final Pin pin = new Pin(hovered.copy(), List.copyOf(hoveredComponents), px, py, hoveredEntry);
        pin.sx = hoveredSx;
        pin.sy = hoveredSy;
        pin.tx = hoveredTx;
        pin.ty = hoveredTy;
        pin.mouseX = hoveredMouseX;
        pins.add(pin);
        suppressStack = hovered.copy();
        suppressLine = firstLine(hoveredComponents);
        suppressSeen = TooltipAnimationState.frame();
        TooltipAnimationState.clear();
    }

    /**
     * Decade: whether this main tooltip is the one just pinned, still hovered: it is not drawn, the pin stands in its
     * place. Once a frame passes without it (the mouse left), it shows again as usual.
     */
    public static boolean suppressed(TooltipContext context) {
        if (suppressLine == null) {
            return false;
        }
        final boolean same = ItemStack.isSameItemSameTags(suppressStack, context.getStack())
                && suppressLine.equals(firstLine(context.getComponents()));
        if (same && TooltipAnimationState.frame() - suppressSeen <= 1) {
            suppressSeen = TooltipAnimationState.frame();
            return true;
        }
        suppressLine = null;
        suppressStack = null;
        return false;
    }

    private static String firstLine(List<ClientTooltipComponent> components) {
        final StringBuilder out = new StringBuilder();
        if (!components.isEmpty() && components.get(0) instanceof net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip text) {
            ((dev.xylonity.tooltipoverhaul.mixin.ClientTextTooltipAccessor) text).getText().accept((i, style, cp) -> {
                out.appendCodePoint(cp);
                return true;
            });
        }
        return out.toString();
    }

    private static Pin pinAt(double mx, double my) {
        final Minecraft client = Minecraft.getInstance();
        if (!allowed(client.screen)) {
            return null;
        }

        for (int i = pins.size() - 1; i >= 0; i--) {
            if (pins.get(i).contains(mx, my)) {
                return pins.get(i);
            }

        }

        return null;
    }

    private static boolean toggle(double mx, double my) {
        final Minecraft client = Minecraft.getInstance();
        if (!allowed(client.screen)) {
            return false;
        }

        attach(client.screen);

        final Pin over = pinAt(mx, my);
        if (over != null) {
            unpin(over);
            return true;
        }

        // Decade: what was hovered, with a stack or without
        if (!hoveredComponents.isEmpty() && System.nanoTime() - lastHover <= 250_000_000L) {
            // only a tooltip with something to open is pinned (the owner, 2026-10-19); the key is taken either way, as it
            // was before, so it does not fall through to the screen (F over a slot would swap the item into the offhand)
            if (dev.xylonity.tooltipoverhaul.client.decade.DecadeNouns.jumpable(hoveredEntry, hoveredComponents)) {
                pinHovered();
            }
            return true;
        }

        if (!pins.isEmpty()) {
            unpin(pins.get(pins.size() - 1));
            return true;
        }

        return false;
    }

    private static double mouseX(Minecraft client) {
        return client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth();
    }

    private static double mouseY(Minecraft client) {
        return client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight();
    }

    public static boolean key(int key, int scanCode, int action) {
        final Minecraft client = Minecraft.getInstance();
        if (!allowed(client.screen) || action != GLFW.GLFW_PRESS) {
            return false;
        }

        if (!TooltipOverhaulKeyMappings.PIN_TOOLTIP.matches(key, scanCode)) {
            return false;
        }

        if (client.screen.getFocused() instanceof EditBox) {
            return false;
        }

        // Decade: not while typing in Decade's own fields, and not with Ctrl held (Ctrl+F is JEI's)
        if (DecadeHooks.typing() || Screen.hasControlDown()) {
            return false;
        }

        return toggle(mouseX(client), mouseY(client));
    }

    public static boolean mouseButton(double mx, double my, int button, int action) {
        if (action == GLFW.GLFW_RELEASE && consumedButton == button) {
            consumedButton = -1;
            // Decade: a press on the body released without moving is a click: the pin's handbook entry
            if (pressed != null && dragging == null && Math.abs(mx - pressX) < 3 && Math.abs(my - pressY) < 3) {
                // a noun opens its own entry; anywhere else, the pin's
                String noun = null;
                for (dev.xylonity.tooltipoverhaul.client.decade.DecadeNouns.Box box : pressed.nouns) {
                    if (box.contains(pressed.lx(mx), pressed.ly(my))) {
                        noun = box.entry();
                    }
                }
                DecadeHooks.open(noun != null ? noun : pressed.entry);
            }
            pressed = null;
            dragging = null;
            return true;
        }

        if (action != GLFW.GLFW_PRESS) {
            return false;
        }

        if (TooltipOverhaulKeyMappings.PIN_TOOLTIP.matchesMouse(button) && toggle(mx, my)) {
            consumedButton = button;
            return true;
        }

        final Pin pin = pinAt(mx, my);
        if (pin == null) {
            return false;
        }

        consumedButton = button;

        // Decade: in the pin's own coordinates (it may be drawn scaled, as the screen drew the tooltip)
        final double lx = pin.lx(mx), ly = pin.ly(my);

        // Decade: pressing the body waits to see whether it is a click or a drag (render() starts the drag once it moves)
        if (button == 0 && ly >= pin.top) {
            pins.remove(pin);
            pins.add(pin);
            pressed = pin;
            pressX = mx;
            pressY = my;
            dragX = lx - pin.x;
            dragY = ly - pin.y;
            return true;
        }

        if (button == 0 && ly < pin.top) {
            if (lx >= pin.right - 16) {
                unpin(pin);
            }
            else {
                pins.remove(pin);
                pins.add(pin);
                dragging = pin;
                dragX = lx - pin.x;
                dragY = ly - pin.y;
            }

        }

        return true;
    }

    public static boolean scroll(double mx, double my, double delta) {
        final Pin pin = pinAt(mx, my);
        if (pin == null) {
            return false;
        }

        final TooltipScrollState.Snapshot previous = TooltipScrollState.snapshot();
        try {
            TooltipScrollState.restore(pin.scrolling);
            TooltipScrollState.onRawScroll(delta);
            pin.scrolling = TooltipScrollState.snapshot();
        }
        finally {
            TooltipScrollState.restore(previous);
        }

        return true;
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY) {
        final Minecraft client = Minecraft.getInstance();
        if (pins.isEmpty() || !allowed(client.screen)) {
            return;
        }

        if (pinnedPlayer != null && pinnedPlayer != client.player) {
            pins.clear();
            pinnedPlayer = null;
            dragging = null;
            return;
        }

        attach(client.screen);

        // Decade: a press on the body becomes a drag once the mouse has moved
        if (pressed != null && dragging == null && (Math.abs(mouseX - pressX) >= 3 || Math.abs(mouseY - pressY) >= 3)) {
            dragging = pressed;
        }

        if (dragging != null) {
            dragging.x = (float) (dragging.lx(mouseX) - dragX);
            dragging.y = (float) (dragging.ly(mouseY) - dragY);
        }

        for (final Pin pin : new ArrayList<>(pins)) {
            render(graphics, pin);
        }

    }

    private static void render(GuiGraphics graphics, Pin pin) {
        final Minecraft client = Minecraft.getInstance();
        final int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
        final TooltipScrollState.Snapshot previous = TooltipScrollState.snapshot();
        final float previousCounter = TooltipRenderer.COUNTER;
        final float previousIconCounter = TooltipRenderer.ICON_COUNTER;

        graphics.pose().pushPose();
        try {
            // Decade: drawn as the screen drew the tooltip (scaled on the market board)
            graphics.pose().translate(pin.tx, pin.ty, 3000);
            graphics.pose().scale(pin.sx, pin.sy, 1);

            TooltipScrollState.restore(pin.scrolling);

            TooltipRenderer.COUNTER = Math.max(0, System.nanoTime() - pin.pinnedAtNano) / 1_000_000_000f;
            TooltipRenderer.ICON_COUNTER = TooltipRenderer.COUNTER;
            final List<ClientTooltipComponent> components = liveComponents(graphics, pin, width, height);
            final TooltipContext context = new TooltipContext(graphics, client.font, components, 0, 0, width,
                    height, DefaultTooltipPositioner.INSTANCE, pin.stack, false);

            context.setPinned(true);

            dev.xylonity.tooltipoverhaul.client.decade.CornerBook.pinEntry = pin.entry;
            final TooltipRenderer renderer = new TooltipRenderer(context);
            renderer.init();
            if (!renderer.canRender()) {
                return;
            }

            final float tooltipWidth = context.getTooltipSize().x;
            final float tooltipHeight = context.getTooltipSize().y;
            // Decade: the screen's edges in the pin's own coordinates
            final float minX = (float) pin.lx(0), maxX = (float) pin.lx(width);
            final float minY = (float) pin.ly(0), maxY = (float) pin.ly(height);
            for (int pass = 0; pass < 2; pass++) {
                context.setTooltipPosition(new Vec2(pin.x, pin.y));
                bounds(context, pin, tooltipWidth, tooltipHeight);
                pin.x += Math.max(minX + 4 - pin.left, Math.min(0, maxX - 4 - pin.right));
                pin.y += Math.max(minY + BADGE_HEIGHT + 6 - pin.top, Math.min(0, maxY - 4 - pin.bottom));
            }

            context.setTooltipPosition(new Vec2(pin.x, pin.y));
            bounds(context, pin, tooltipWidth, tooltipHeight);

            graphics.enableScissor((int) pin.sx(pin.left - 8), (int) pin.sy(pin.top - BADGE_HEIGHT - 10),
                    (int) Math.ceil(pin.sx(pin.right + 8)), (int) Math.ceil(pin.sy(pin.bottom + 8)));
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            graphics.disableScissor();

            // Decade: the pin's noun boxes, found while its lines are drawn
            dev.xylonity.tooltipoverhaul.client.decade.DecadeNouns.begin(pin.lx(mouseX(client)), pin.ly(mouseY(client)), pin.entry);
            TooltipAnimator.render(context, false, 1);
            pin.nouns = dev.xylonity.tooltipoverhaul.client.decade.DecadeNouns.end();

            graphics.flush();

            graphics.pose().translate(0, 0, 2600);

            // Badge above the tooltip (Decade: kept, the owner 2026-10-19: its x is how a pin is closed)
            final int badgeTop = (int) pin.top - BADGE_HEIGHT - 2;
            final int badgeBottom = (int) pin.top - 2;
            graphics.fill((int) pin.left, badgeTop, (int) pin.right, badgeBottom, 0xF0343B4A);
            graphics.fill((int) pin.left + 1, badgeTop + 1, (int) pin.right - 1, badgeBottom - 1, 0xF0222833);
            graphics.drawString(client.font, client.font.plainSubstrByWidth(Component.translatable("tooltipoverhaul.pinned.title").getString(), Math.max(1, (int) (pin.right - pin.left - 25))), (int) pin.left + 4, badgeTop + 4, 0xFFBFD7F4, false);
            graphics.drawString(client.font, "x", (int) pin.right - 11, badgeTop + 4, 0xFFFFFFFF, false);

            pin.scrolling = TooltipScrollState.snapshot();
        }
        catch (RuntimeException exception) {
            TooltipOverhaul.LOGGER.warn("Could not render pinned tooltip", exception);
            unpin(pin);
        }
        finally {
            graphics.flush();
            graphics.pose().popPose();
            dev.xylonity.tooltipoverhaul.client.decade.CornerBook.pinEntry = null;
            TooltipRenderer.COUNTER = previousCounter;
            TooltipRenderer.ICON_COUNTER = previousIconCounter;
            TooltipScrollState.restore(previous);
        }

    }

    /**
     * Updates the pinned tooltip per tick
     */
    private static List<ClientTooltipComponent> liveComponents(GuiGraphics graphics, Pin pin, int width, int height) {
        // Decade: a text tooltip has no stack to rebuild it from: what was pinned stays
        if (pin.stack.isEmpty()) {
            return pin.components;
        }

        try {
            // Decade: built as the hovered tooltip was, for the mouse x it was laid out for
            List<ClientTooltipComponent> components = TooltipOverhaul.PLATFORM.itemTooltip(pin.stack, Minecraft.getInstance().font, pin.mouseX, width, height);
            components = TooltipOverhaul.PLATFORM.gatherTooltipComponents(graphics, pin.stack, components, Minecraft.getInstance().font, (int) pin.x, (int) pin.y, width, height, DefaultTooltipPositioner.INSTANCE);
            if (!components.isEmpty()) {
                pin.components = List.copyOf(components);
                pin.refreshWarningShown = false;
                return components;
            }

        }
        catch (RuntimeException exception) {
            if (!pin.refreshWarningShown) {
                TooltipOverhaul.LOGGER.warn("Could not refresh pinned tooltip contents", exception);
                pin.refreshWarningShown = true;
            }

        }

        return pin.components;
    }

    private static void bounds(TooltipContext context, Pin pin, float width, float height) {
        pin.left = pin.x - 4; pin.top = pin.y - 4; pin.right = pin.x + width + 4; pin.bottom = pin.y + height + 4;
        if (TooltipLayout.hasExternalIcon(context)) {
            final float ix = pin.x + TooltipLayout.iconX(context), iy = pin.y + TooltipLayout.iconY(context);
            pin.left = Math.min(pin.left, ix - 4); pin.top = Math.min(pin.top, iy - 4);
            pin.right = Math.max(pin.right, ix + Constants.getIconSize(context) + 4);
            pin.bottom = Math.max(pin.bottom, iy + Constants.getIconSize(context) + 4);
        }

    }

    private static final class Pin {

        private final ItemStack stack;
        private List<ClientTooltipComponent> components;
        // Decade: the handbook entry a click opens, or null; the nouns in it, as last drawn
        private final String entry;
        private List<dev.xylonity.tooltipoverhaul.client.decade.DecadeNouns.Box> nouns = List.of();
        // Decade: screen = pin coordinates × s + t, as the screen drew the hovered tooltip; the mouse x it was built for
        private float sx = 1, sy = 1, tx, ty;
        private int mouseX;
        private final long pinnedAtNano;
        private boolean refreshWarningShown;
        private float x, y;
        private float left, top, right, bottom;
        private TooltipScrollState.Snapshot scrolling = new TooltipScrollState.Snapshot(0, 0, false, 0, 0, 0);

        private Pin(ItemStack stack, List<ClientTooltipComponent> components, float x, float y, String entry) {
            this.stack = stack;
            this.entry = entry;
            this.components = components;
            this.pinnedAtNano = System.nanoTime();
            this.x = x;
            this.y = y;
        }

        private double lx(double mx) {
            return (mx - tx) / sx;
        }

        private double ly(double my) {
            return (my - ty) / sy;
        }

        private float sx(float x) {
            return x * sx + tx;
        }

        private float sy(float y) {
            return y * sy + ty;
        }

        private boolean contains(double mx, double my) {
            final double x = lx(mx), y = ly(my);
            return x >= left && x <= right && y >= top - BADGE_HEIGHT - 2 && y <= bottom;
        }

    }

}
