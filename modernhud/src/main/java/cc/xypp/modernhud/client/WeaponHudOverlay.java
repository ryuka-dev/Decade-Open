package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.client.compat.TaczCompat;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import cc.xypp.modernhud.config.WeaponConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * TACZ weapon widget, Call-of-Duty style (Modern Warfare's bottom-right ammo counter): mag and
 * reserve numbers side by side ("051 | 108" — reserve rendered smaller and slightly dimmer than
 * the mag number, each with its own leading-zero digit(s) rendered faint/dim relative to its own
 * significant digits when {@link WeaponConfig#zeroPadAmmo} is on — see {@link #drawZeroPaddedNumber}),
 * plus a small "ammo type" icon next to reserve (see {@link WeaponConfig#showAmmoTypeIcon}) — its
 * own independent element (see {@link #drawAmmoRow}), background a soft GLOW rather than a flat
 * shadowbox, with corner brackets sitting OUTSIDE its edge (see {@link WeaponConfig#borderOutset}).
 * The weapon's name sits directly underneath as its own plain floating text with NO background of
 * its own (see {@link #drawNameRow}) — the ammo row's glow does not extend down over it. Fire mode
 * is its own segmented control below that (see {@link #drawFireModeSegmented}): one shared box
 * divided into equal-width cells, one per supported mode, with the ACTIVE mode's cell getting an
 * extra highlight border on top of the shared box's own — all per close comparison against the
 * reference HUD, where these three are visually independent, not one shared box.
 *
 * <p>The gun's own TACZ HUD icon sits on the right in its own separate box, with a row of small
 * attachment-slot squares directly above it (see {@link #drawAttachmentDots},
 * {@link WeaponConfig#showAttachmentDots} — one square per slot this SPECIFIC gun actually has,
 * filled gold for equipped, dim for empty), and a small, dim secondary-weapon box tucked BELOW the
 * icon (same width as the primary box, icon right-aligned inside it, reserve ammo count
 * left-aligned) for the last different gun you were holding (Call of Duty's primary/secondary
 * weapon slots) — or, while carrying but not holding any gun, for up to two guns found in the
 * hotbar (see {@link #renderNeutral}). The secondary/neutral box's own accent is deliberately more
 * minimal than the primary's: two small dots at its BOTTOM corners only, nothing at the top (see
 * {@link HudPanel#drawBottomDots}, {@link WeaponConfig#secondaryDotSize}) — matching the reference
 * HUD's less prominent treatment for a slot that isn't the one actively being fired.
 *
 * <p>No gradient/fade on the fills, just a flat colour (or, for the ammo row specifically, a soft
 * glow) at an adjustable opacity, plus an optional edge accent on top — none, small corner
 * brackets (the default, modelled on the referenced MW4 HUD), or a full thin outline — see
 * {@link WeaponConfig#borderStyle}. The icon box(es) and fire-mode box keep a dark default fill
 * ({@link WeaponConfig#panelColor}); the ammo row defaults to a light off-white glow instead
 * ({@link WeaponConfig#textPanelColor}), matching Modern Warfare's own lighter, more subtle
 * backdrop behind its ammo readout.
 *
 * <p>The "push up into place" switch animation applies ONLY to the icon box(es) (and the
 * attachment dots/secondary box that move with it) — the ammo/name/fire-mode elements are drawn in
 * a separate, un-translated pose push, so they never slide around on a weapon swap, only the icon
 * graphic does. Independently toggle-able
 * ({@link WeaponConfig#showSwitchSlide}) — the slide itself now eases with a quintic ease-out curve
 * (see {@link EasedAnimator}) for a snappier, tighter settle than this used to have. The same
 * switch event ALSO triggers, independently of each other and of the slide: a brief bright glow
 * flash behind the primary icon (see {@link GlowEffect}, {@link WeaponConfig#showWeaponGlow}) that
 * outlasts the slide slightly so the icon visibly "settles" rather than the animation just cutting
 * off, and a gold border flash around the primary icon's own border (see
 * {@link WeaponConfig#showSwitchBorderFlash}) modelled on vanilla Minecraft's own gold-flash accents
 * (a totem of undying's screen flash, for instance), fading back to the normal border colour. The
 * combined ammo chip separately gets a slow breathing glow pulse (same colour as the low-ammo mag
 * number) while critically low on ammo. Separately, a centre-screen "FIRE TYPE: FULL AUTO"
 * announcement (see {@link #drawFireTypeFlash}) flashes above the crosshair on every weapon switch
 * AND every time you cycle fire mode on the gun you're already holding, independent of the widget's
 * own screen position - matching Modern Warfare's own weapon-pull announcement.
 *
 * <p>Holding something other than a TACZ gun doesn't hide the widget outright: as long as at least
 * one gun is still sitting somewhere in the hotbar, up to two of them (ordered by hotbar slot, not
 * by switch history) keep showing as a pair of small, dim, equally-unhighlighted icons with their
 * reserve ammo counts — see {@link #renderNeutral} — with no name/mag readout, since neither is
 * actually being held/fired right now. Switch-history ({@link #currentGunId}/{@link #secondaryGunId})
 * is left untouched while in this state, so picking a real gun back up resumes primary/secondary
 * tracking right where it left off.
 *
 * <p><b>Layout is FIXED</b> — the panel is one constant size (see {@link WeaponConfig}), never
 * resized by the current gun's name length or icon art, and its screen position is config-file-only
 * (no in-game drag handle — see {@code HudEditorScreen}'s class javadoc for why). TACZ HUD-icon
 * textures and gun-name lengths vary wildly gun to gun, so sizing the frame off either one made the
 * whole widget balloon, overlap the crosshair, and run off the edge of the screen for long names.
 *
 * <p>Registered only when TACZ is loaded (see {@code ClientEvents}), and TACZ's own built-in gun
 * HUD is switched off in favor of this one (see {@link TaczCompat#setVanillaGunHudEnabled}) so the
 * two never render on top of each other. Even then this renders nothing unless the held item is
 * actually a gun.
 */
public class WeaponHudOverlay implements IGuiOverlay {

    private static final ResourceLocation FIRE_ICON_SEMI = new ResourceLocation(ModernHud.MOD_ID, "textures/hud/fire_mode_semi.png");
    private static final ResourceLocation FIRE_ICON_AUTO = new ResourceLocation(ModernHud.MOD_ID, "textures/hud/fire_mode_auto.png");
    private static final ResourceLocation FIRE_ICON_BURST = new ResourceLocation(ModernHud.MOD_ID, "textures/hud/fire_mode_burst.png");

    private static final int PAD = 6;
    private static final int ICON_BOX_W = 62;
    private static final int ICON_BOX_H = 36;
    /** Gap between the primary icon box and the secondary box directly below it - tightened from
     *  the previous 4px per feedback that the widget's spacing read as too loose. */
    private static final int SECONDARY_GAP = 2;
    private static final int SECONDARY_BOX_H = 18;
    private static final int FIRE_ICON_SIZE = 9;
    private static final int FIRE_ICON_GAP = 4;
    /** Real full size of the fire-mode icon source PNGs (see {@link #FIRE_ICON_AUTO} etc.) - unlike
     *  {@link #FIRE_ICON_SIZE}, this is the texture's TRUE pixel dimensions, needed for a correct UV
     *  crop (see {@link #drawAmmoTypeIcon}), not the small size those icons happen to be drawn at
     *  elsewhere on this widget. */
    private static final int FIRE_ICON_TEX_SIZE = 128;
    /** Tight crop (in real texture pixels) of the actual icon content within
     *  {@link #FIRE_ICON_AUTO}'s padded {@link #FIRE_ICON_TEX_SIZE}x{@code FIRE_ICON_TEX_SIZE}
     *  canvas — measured directly against the source PNG's alpha channel, plus a 1px safety margin.
     *  Sampling only this region (rather than the whole padded canvas, as the fire-mode row's own
     *  icons still do at {@link #FIRE_ICON_SIZE}) means the destination box is spent entirely on
     *  real icon detail instead of blank space, which is what read as mushy/warped at a small size -
     *  see {@link #drawAmmoTypeIcon}. */
    private static final int FIRE_ICON_CROP_U = 33;
    private static final int FIRE_ICON_CROP_V = 30;
    private static final int FIRE_ICON_CROP_W = 68;
    private static final int FIRE_ICON_CROP_H = 68;
    /** Small squares near the top of the primary icon box showing equipped attachments - see
     *  {@link WeaponConfig#showAttachmentDots}. */
    private static final int ATTACHMENT_DOT_SIZE = 4;
    private static final int ATTACHMENT_DOT_GAP = 3;

    /** Outer padding of the combined ammo/name/fire-mode chip, and of each icon box, around their
     *  content — kept tight on purpose for the "sleek, modern" look, not a big roomy box. */
    private static final int CHIP_PAD_X = 4;
    private static final int CHIP_PAD_Y = 2;
    /** Gap between the combined chip's internal rows (mag|reserve, name, fire-mode) - tightened
     *  from the previous 3px inter-chip gap now that they share one box instead of three. */
    private static final int ROW_GAP = 2;

    private ResourceLocation currentGunId;
    /** Decade: the weapon slot in hand last frame, -1 when none - a swap between two guns of the
     *  same model is still a swap. */
    private int currentSlot = -1;
    /** Decade: how far up from the bottom of the screen the widget reached last frame, 0 when it was
     *  not drawn; handed to TaCZ so the subtitles move above it (see {@code ClientEvents}). */
    private static volatile int reservedHeight;
    private ResourceLocation secondaryGunId;
    /** Shared timer for the "push up into place" animation, the fire-type flash announcement, and
     *  (implicitly, since it's drawn every frame while a gun is held) everything else that's timed
     *  off a weapon switch - they're all triggered by the same event, so one timer covers all of it. */
    private long switchAnimStartMs = Long.MIN_VALUE / 2; // safely "long ago", avoids overflow in elapsed math
    private static final long PUSH_ANIM_MS = 220L;
    /** Duration of the bright glow flash behind the primary icon on weapon switch - see
     *  {@link WeaponConfig#showWeaponGlow}. Longer than the push animation itself so the glow
     *  visibly outlasts the slide, reading as "the icon just settled" rather than cutting off
     *  the instant the slide finishes. */
    private static final long SWITCH_GLOW_MS = 400L;

    /** Fire mode of the currently-held gun as of last frame - lets {@link #render} tell "you cycled
     *  fire mode on the SAME gun" apart from "you switched to a different gun" (the latter already
     *  has its own trigger, {@link #switchAnimStartMs}). Reset to {@code null} on a weapon switch so
     *  the new gun's mode is never compared against the old gun's. */
    private String lastFireModeSeen;
    /** Timer for a fire-mode change on the currently-held gun - see {@link #lastFireModeSeen}. Only
     *  drives the "FIRE TYPE: ..." announcement ({@link #drawFireTypeFlash}); deliberately doesn't
     *  touch the icon push/glow/border-flash above, since those are specifically about a weapon
     *  swap, not a fire-mode cycle on the gun you're already holding. */
    private long fireModeChangeStartMs = Long.MIN_VALUE / 2;

    /** How often (ms) we re-poke TACZ's own "show built-in gun HUD" toggle off while our widget is
     *  actively drawing. Fixes the "HUD đôi lúc bị đè lên nhau" report: something else (TACZ
     *  reloading its client config on a gunpack switch, another addon touching the same flag for
     *  its own vehicle/turret integration, etc.) can flip {@code RenderConfig.GUN_HUD_ENABLE} back
     *  on after our one-time sync at config-load. Re-asserting it every second while we're the
     *  active weapon HUD is cheap (a handful of reflection calls) and makes TACZ's own HUD unable
     *  to "win back" the screen for more than about a second at a time. */
    private static final long VANILLA_HUD_REASSERT_MS = 1000L;
    private long lastVanillaHudReassertMs = Long.MIN_VALUE / 2;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        reservedHeight = 0; // set again below by whichever path actually draws
        if (!WeaponConfig.enabled || !TaczCompat.loaded()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.hideGui) {
            return;
        }
        if (player.isSpectator()) {
            // Only this mod's own weapon widget is skipped here - vanilla's HUD/hotbar rendering
            // is a completely separate code path we never touch, so it's unaffected either way.
            currentGunId = null;
            return;
        }
        if (SuperbWarfareCompat.isInVehicle(player)) {
            // Riding an SBW vehicle - it draws its own vehicle/weapon HUD over the same bottom-right
            // area, for every seat (driver, gunner, passenger), so ours steps aside entirely rather
            // than overlap it. Covers the "neutral" carrying-guns fallback below too, since that
            // early-returns right here along with the normal held-gun path.
            currentGunId = null;
            return;
        }
        ItemStack held = player.getMainHandItem();
        int weaponSlots = TaczCompat.weaponSlots();
        int selected = player.getInventory().selected;
        if (weaponSlots > 0 && (selected >= weaponSlots || held.isEmpty() || !TaczCompat.isGun(held))) {
            // Decade: a gun outside the weapon slots cannot be used, so only the slots show, dim
            currentGunId = null;
            currentSlot = -1;
            renderNeutralSlots(graphics, player, weaponSlots, screenWidth, screenHeight);
            return;
        }
        if (held.isEmpty() || !TaczCompat.isGun(held)) {
            currentGunId = null;
            renderNeutral(graphics, player, screenWidth, screenHeight);
            return;
        }
        Font font = Minecraft.getInstance().font;

        if (weaponSlots > 0) {
            trackBySlot(held, selected);
        } else {
            trackWeaponSlots(player, held);
        }
        reassertVanillaHudOff();

        // Cycling fire mode on the SAME gun (e.g. TACZ's own fire-mode key) should replay the
        // "FIRE TYPE: ..." announcement too, not just switching to a different gun - see
        // fireModeChangeStartMs/lastFireModeSeen. Runs every frame a gun is held, independent of
        // trackWeaponSlots' own switch detection above.
        String heldFireMode = TaczCompat.fireMode(held);
        if (lastFireModeSeen != null && heldFireMode != null && !heldFireMode.equals(lastFireModeSeen)) {
            fireModeChangeStartMs = Util.getMillis();
        }
        lastFireModeSeen = heldFireMode;

        int frameW = WeaponConfig.frameWidth;
        int frameH = WeaponConfig.frameHeight;

        int[] pos = resolvePosition(screenWidth, screenHeight, frameW, frameH, WeaponConfig.scale);
        int originX = pos[0];
        int originY = pos[1];
        reservedHeight = screenHeight - originY;

        int iconBoxX = frameW - ICON_BOX_W;
        int textRightEdge = iconBoxX - PAD;

        // "Push up into place" on weapon switch: starts switchSlideDistance px lower and eases up
        // to its resting position over PUSH_ANIM_MS, using the same switch timer as the name toast.
        // Applies ONLY to the icon box graphic(s) below - the ammo numbers, reserve, and fire-mode
        // row stay put and never slide, so the readable numbers don't jump around on a swap.
        // Quintic ease-out (matches EasedAnimator's default curve) for a snappier "premium" settle
        // than the plain quad curve this used to use. Independently toggle-able (showSwitchSlide) -
        // per feedback that the slide alone felt "loose" for some tastes, this can run alongside,
        // or instead of, the border flash below.
        long switchElapsed = Util.getMillis() - switchAnimStartMs;
        int pushOffset = 0;
        if (WeaponConfig.showSwitchSlide) {
            float animT = switchElapsed >= PUSH_ANIM_MS ? 1f : Math.max(0f, (float) switchElapsed / PUSH_ANIM_MS);
            float invT = 1f - animT;
            float eased = 1f - invT * invT * invT * invT * invT;
            pushOffset = Math.round((1f - eased) * WeaponConfig.switchSlideDistance);
        }

        // Same switch event drives a brief glow flash behind the primary icon (see SWITCH_GLOW_MS) -
        // an independent, longer-running fade so the glow visibly outlasts the slide itself.
        float switchGlowAlpha = 0f;
        if (WeaponConfig.showWeaponGlow && switchElapsed >= 0 && switchElapsed < SWITCH_GLOW_MS) {
            float glowT = 1f - (float) switchElapsed / SWITCH_GLOW_MS;
            switchGlowAlpha = glowT * glowT; // squared for a punchier falloff than linear
        }

        // ...and a gold border flash on the primary icon's own border, fading back to the normal
        // border colour - modelled on vanilla Minecraft's own gold-flash accents (a totem of
        // undying's screen flash, for instance). Drawn as an EXTRA border pass layered on top of
        // the normal one at fading opacity, rather than colour-interpolated, so it naturally reads
        // as "gold fading back to normal" as less of it shows through.
        float switchBorderFlashAlpha = 0f;
        if (WeaponConfig.showSwitchBorderFlash && switchElapsed >= 0 && switchElapsed < WeaponConfig.switchBorderFlashMs) {
            float flashT = 1f - (float) switchElapsed / WeaponConfig.switchBorderFlashMs;
            switchBorderFlashAlpha = flashT * flashT;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(originX, originY, 0);
        pose.scale((float) WeaponConfig.scale, (float) WeaponConfig.scale, 1.0f);

        int ammoBottom = drawAmmoRow(graphics, font, player, held, textRightEdge);
        int nameBottom = drawNameRow(graphics, font, held, textRightEdge, ammoBottom + ROW_GAP);
        drawFireModeSegmented(graphics, font, held, textRightEdge, nameBottom + ROW_GAP);

        pose.pushPose();
        pose.translate(0, pushOffset, 0);
        drawPrimaryIcon(graphics, held, iconBoxX, 0, ICON_BOX_W, ICON_BOX_H, switchGlowAlpha, switchBorderFlashAlpha);
        if (WeaponConfig.showAttachmentDots) {
            // Drawn AFTER (so on top of) the icon, INSIDE the box's own bounds near the top edge -
            // not a separate row above it, which previously left the box's own frame with nothing
            // visibly connecting the dots to it.
            drawAttachmentDots(graphics, held, iconBoxX, 0, ICON_BOX_W);
        }
        if (WeaponConfig.showSecondary) {
            if (weaponSlots > 0) {
                drawOtherSlot(graphics, player, weaponSlots, selected, iconBoxX, ICON_BOX_H + SECONDARY_GAP);
            } else {
                drawSecondaryWeapon(graphics, player, iconBoxX, ICON_BOX_H + SECONDARY_GAP);
            }
        }
        pose.popPose();

        pose.popPose();

        if (WeaponConfig.showFireTypeFlash) {
            // Drawn OUTSIDE the widget's own pose/scale on purpose - this is a centre-screen,
            // above-the-crosshair announcement, entirely independent of where the widget itself
            // sits on screen (see the class javadoc).
            drawFireTypeFlash(graphics, font, held, screenWidth, screenHeight);
        }
    }

    /** See {@link #VANILLA_HUD_REASSERT_MS}. Throttled so this is a once-a-second cost, not
     *  once-a-frame. */
    private void reassertVanillaHudOff() {
        long now = Util.getMillis();
        if (now - lastVanillaHudReassertMs < VANILLA_HUD_REASSERT_MS) {
            return;
        }
        lastVanillaHudReassertMs = now;
        WeaponConfig.syncHudSource();
    }

    /**
     * Not holding a TACZ gun right now (pickaxe, block, bare hands...) — but the widget doesn't
     * just vanish if you're still CARRYING one or two guns elsewhere in the hotbar. Shows up to two
     * of them, ordered by hotbar slot (earliest slot first), both at the same reduced/dim scale the
     * secondary slot normally uses — neither highlighted as "the" primary, since you aren't
     * actually holding either one. No name/mag readout (there's nothing to read when nothing's in
     * hand), but each DOES show its reserve ammo count, same as the normal secondary slot does.
     * {@link #currentGunId}/{@link #secondaryGunId} (the switch-history used while actually holding
     * a gun) are deliberately left untouched here, so picking a real gun back up resumes
     * primary/secondary tracking exactly where it left off.
     */
    private void renderNeutral(GuiGraphics graphics, LocalPlayer player, int screenWidth, int screenHeight) {
        if (!WeaponConfig.enabled || !TaczCompat.loaded() || Minecraft.getInstance().options.hideGui) {
            return;
        }
        ResourceLocation[] guns = findGunsInHotbarOrdered(player.getInventory(), 2);
        if (guns.length == 0) {
            reservedHeight = 0;
            return; // nothing carried right now - genuinely nothing to show
        }
        reassertVanillaHudOff();

        int frameW = WeaponConfig.frameWidth;
        int[] pos = resolvePosition(screenWidth, screenHeight, frameW, WeaponConfig.frameHeight, WeaponConfig.scale);
        reservedHeight = screenHeight - pos[1];
        int iconBoxX = frameW - ICON_BOX_W;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(pos[0], pos[1], 0);
        pose.scale((float) WeaponConfig.scale, (float) WeaponConfig.scale, 1.0f);

        int boxX = iconBoxX; // secondary/neutral box width now matches the primary box exactly
        int firstY = (ICON_BOX_H - SECONDARY_BOX_H) / 2; // centred where the primary icon normally sits
        drawNeutralIcon(graphics, player, guns[0], boxX, firstY);
        if (guns.length > 1) {
            drawNeutralIcon(graphics, player, guns[1], boxX, firstY + SECONDARY_BOX_H + SECONDARY_GAP);
        }

        pose.popPose();
    }

    /**
     * Decade: the primary is the gun in the weapon slot in hand; a swap is a different slot or a
     * different gun in it. Replaces {@link #trackWeaponSlots}' "last different gun held" while the
     * weapon slots are known.
     */
    private void trackBySlot(ItemStack held, int selected) {
        ResourceLocation gunId = TaczCompat.gunId(held);
        if (gunId == null) {
            return;
        }
        if (selected != currentSlot || !gunId.equals(currentGunId)) {
            currentSlot = selected;
            currentGunId = gunId;
            switchAnimStartMs = Util.getMillis();
            lastFireModeSeen = null;
        }
    }

    /** Decade: the secondary box is the first other weapon slot that holds a gun, if any. */
    private void drawOtherSlot(GuiGraphics g, LocalPlayer player, int weaponSlots, int selected, int iconBoxX, int stripY) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < weaponSlots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (slot != selected && !stack.isEmpty() && TaczCompat.isGun(stack)) {
                drawCarriedIcon(g, player, stack, iconBoxX, stripY, WeaponConfig.secondaryAlpha);
                return;
            }
        }
    }

    /**
     * Decade: {@link #renderNeutral} by weapon slot - not holding a gun from one, the guns in the
     * weapon slots (up to two, in slot order) show dim with their reserve, nothing else.
     */
    private void renderNeutralSlots(GuiGraphics graphics, LocalPlayer player, int weaponSlots, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().options.hideGui) {
            reservedHeight = 0;
            return;
        }
        Inventory inventory = player.getInventory();
        java.util.List<ItemStack> guns = new java.util.ArrayList<>(2);
        for (int slot = 0; slot < weaponSlots && guns.size() < 2; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && TaczCompat.isGun(stack)) {
                guns.add(stack);
            }
        }
        if (guns.isEmpty()) {
            reservedHeight = 0;
            return;
        }
        reassertVanillaHudOff();
        int frameW = WeaponConfig.frameWidth;
        int[] pos = resolvePosition(screenWidth, screenHeight, frameW, WeaponConfig.frameHeight, WeaponConfig.scale);
        reservedHeight = screenHeight - pos[1];
        int iconBoxX = frameW - ICON_BOX_W;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(pos[0], pos[1], 0);
        pose.scale((float) WeaponConfig.scale, (float) WeaponConfig.scale, 1.0f);
        int firstY = (ICON_BOX_H - SECONDARY_BOX_H) / 2;
        for (int i = 0; i < guns.size(); i++) {
            drawCarriedIcon(graphics, player, guns.get(i), iconBoxX, firstY + i * (SECONDARY_BOX_H + SECONDARY_GAP), WeaponConfig.secondaryAlpha);
        }
        pose.popPose();
    }

    /** Decade: last frame's height from the bottom of the screen, for TaCZ's subtitle offset. */
    public static int reservedHeight() {
        return reservedHeight;
    }

    /** Up to {@code max} DIFFERENT guns found in the hotbar (slots 0-8), in slot order — the
     *  discovery method for {@link #renderNeutral}, deliberately independent of switch-history. */
    private ResourceLocation[] findGunsInHotbarOrdered(Inventory inventory, int max) {
        java.util.List<ResourceLocation> found = new java.util.ArrayList<>(max);
        java.util.Set<ResourceLocation> seen = new java.util.HashSet<>();
        for (int i = 0; i < 9 && found.size() < max; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = TaczCompat.gunId(stack);
            if (id != null && seen.add(id)) {
                found.add(id);
            }
        }
        return found.toArray(new ResourceLocation[0]);
    }

    /** Draws one individually-sized flat-colour background chip, gated on {@link WeaponConfig#showPanel},
     *  plus its optional border accent (see {@code HudPanel#drawBorder}), gated on
     *  {@link WeaponConfig#borderStyle} != NONE - only drawn when the fill above is also on, since it's
     *  layered on top of it, not a replacement. {@code extraAlphaMul} additionally scales BOTH the
     *  fill's and the border's opacity (0-1, e.g. {@link WeaponConfig#secondaryAlpha}) for chips that
     *  should recede further, like the secondary/neutral icon boxes — pass 1.0 for full strength.
     *  Every boxed element on this widget goes through here, so they all share one source of truth. */
    private void drawChipBox(GuiGraphics g, int x, int y, int w, int h, double extraAlphaMul) {
        if (WeaponConfig.showPanel) {
            HudPanel.draw(g, x, y, w, h, WeaponConfig.panelColor, WeaponConfig.panelOpacity * extraAlphaMul);
            HudPanel.drawBorder(g, x, y, w, h, WeaponConfig.borderColor,
                    WeaponConfig.borderOpacity * extraAlphaMul, WeaponConfig.borderStyle, WeaponConfig.borderLength, WeaponConfig.borderOutset);
        }
    }

    /** Same fill as {@link #drawChipBox}, but a more minimal accent: two small dots at the BOTTOM
     *  corners only, nothing at the top corners at all — the secondary/neutral weapon box's own
     *  distinct treatment, matching the reference HUD's less prominent styling for a slot that
     *  isn't the one actively being fired. */
    private void drawSecondaryChipBox(GuiGraphics g, int x, int y, int w, int h, double extraAlphaMul) {
        if (WeaponConfig.showPanel) {
            HudPanel.draw(g, x, y, w, h, WeaponConfig.panelColor, WeaponConfig.panelOpacity * extraAlphaMul);
            HudPanel.drawBottomDots(g, x, y, w, h, WeaponConfig.borderColor,
                    WeaponConfig.borderOpacity * extraAlphaMul, WeaponConfig.secondaryDotSize);
        }
    }

    /**
     * Keeps track of "current gun" vs. "the last different gun you held" (secondary), the same
     * primary/secondary relationship Call of Duty shows: whichever gun is in hand right now is
     * always primary; switching to a different gun demotes the old one to secondary (and picking
     * the old one back up promotes it again). Switching to something that ISN'T a gun (a
     * pickaxe, a block, your fists) is ignored entirely - it neither becomes primary nor bumps
     * the secondary slot, so the last two actual guns stay remembered across non-gun items.
     *
     * <p>Switch-history alone used to be the ONLY way the secondary slot got populated — meaning
     * if you spawned in (or picked up a second gun) already carrying two guns and never actually
     * scrolled between them yet, the secondary box stayed empty even though you were holding two
     * guns right then. Now, whenever switch-history hasn't given us a secondary (or the one it
     * remembered isn't in the hotbar any more), we fall back to scanning the hotbar for whatever
     * OTHER gun is sitting there and show that instead — so both slots populate immediately, no
     * scrolling back and forth required first.
     */
    private void trackWeaponSlots(LocalPlayer player, ItemStack held) {
        ResourceLocation gunId = TaczCompat.gunId(held);
        if (gunId == null) {
            return;
        }
        if (!gunId.equals(currentGunId)) {
            if (currentGunId != null && !currentGunId.equals(gunId)) {
                secondaryGunId = currentGunId;
            }
            currentGunId = gunId;
            switchAnimStartMs = Util.getMillis();
            // Don't let the new gun's fire mode be compared against the old gun's next frame -
            // that's not a "you cycled fire mode" event, and the switch flash above already covers
            // announcing the new gun's mode.
            lastFireModeSeen = null;
        }
        if (gunId.equals(secondaryGunId)) {
            // Picked the secondary weapon back up - it's primary now, and there's nothing to show
            // as secondary until the player switches to some OTHER gun (or the hotbar fallback
            // below finds one).
            secondaryGunId = null;
        }
        if (secondaryGunId == null || findInHotbar(player.getInventory(), secondaryGunId) == null) {
            secondaryGunId = findOtherGunInHotbar(player.getInventory(), gunId);
        }
    }

    /** First gun in the hotbar (slots 0-8) that ISN'T {@code excludeGunId} — the auto-detect
     *  fallback for {@link #trackWeaponSlots}. */
    private ResourceLocation findOtherGunInHotbar(Inventory inventory, ResourceLocation excludeGunId) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = TaczCompat.gunId(stack);
            if (id != null && !id.equals(excludeGunId)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Mag "|" reserve — its OWN independent element now, not part of a shared box with name/
     * fire-mode (per closer inspection of the reference HUD: only the ammo row itself gets a
     * background, the framed area does NOT extend over the name/fire-mode below it). Background is
     * a soft GLOW rather than a flat shadowbox fill, matching the reference's radiant look around
     * the numbers, plus corner brackets sitting OUTSIDE the row (see {@link WeaponConfig#borderOutset}).
     * Reserve renders smaller ({@link WeaponConfig#reserveScaleOffset}) and dimmer
     * ({@link WeaponConfig#reserveOpacity}) than the mag number. When {@link WeaponConfig#zeroPadAmmo}
     * is on, each number's leading zero(s) render at reduced opacity relative to its own significant
     * digits — a "ghost placeholder" look, matching the reference's faint leading "0" in "030".
     * Returns the Y just below this row, for {@link #drawNameRow} to sit under.
     */
    private int drawAmmoRow(GuiGraphics g, Font font, LocalPlayer player, ItemStack gun, int rightEdge) {
        int mag = TaczCompat.magAmmo(player, gun);
        int cap = TaczCompat.magCapacity(gun);
        String magText = ammoString(mag);
        boolean low = cap > 0 && (100.0 * mag / cap) <= WeaponConfig.lowAmmoThresholdPercent;
        int magColor = (low ? WeaponConfig.ammoLowColor : WeaponConfig.ammoColor) | 0xFF000000;

        float ammoScale = (float) WeaponConfig.ammoTextScale;
        float reserveScale = Math.max(0.3f, ammoScale - (float) WeaponConfig.reserveScaleOffset);

        String reserveText = WeaponConfig.showReserveAmmo ? reserveString(player, gun) : "";
        boolean hasReserve = !reserveText.isEmpty();
        String sep = " | ";

        float magW = font.width(magText) * ammoScale;
        float sepW = hasReserve ? font.width(sep) * reserveScale : 0f;
        float reserveW = hasReserve ? font.width(reserveText) * reserveScale : 0f;
        int ammoRowH = Math.round(font.lineHeight * ammoScale);

        int reserveRowH = Math.round(font.lineHeight * reserveScale);

        int iconW = 0;
        int iconGap = 0;
        if (hasReserve && WeaponConfig.showAmmoTypeIcon) {
            // Sized to match the RESERVE digits' own rendered height (reserveRowH), not the larger
            // mag digits' height (ammoRowH) - this icon sits directly next to the reserve number,
            // so it needs to track reserveScale to stay visually balanced with it. Using ammoRowH
            // here made the icon render oversized relative to the (smaller, dimmer) reserve text.
            // On top of that, matching reserveRowH 1:1 (scale 1.0) STILL reads oversized next to
            // the actual glyphs, since the digits' rendered ink is noticeably smaller than the
            // font's full line-height box - so ammoTypeIconScale (<1) shrinks it down further to
            // an actually-balanced size. Still tracks ammoTextScale/reserveScaleOffset instead of
            // a fixed pixel size - see drawAmmoTypeIcon for how it's cropped/scaled cleanly to any size.
            iconW = Math.max(1, Math.round(reserveRowH * (float) WeaponConfig.ammoTypeIconScale));
            iconGap = 4;
        }
        float ammoRowW = magW + sepW + reserveW + iconGap + iconW;

        int reserveAlpha = Math.max(1, (int) Math.round(WeaponConfig.reserveOpacity * 2.55));
        int reserveColor = (reserveAlpha << 24) | (WeaponConfig.reserveColor & 0xFFFFFF);

        int boxW = Math.round(ammoRowW) + CHIP_PAD_X * 2;
        int boxH = ammoRowH + CHIP_PAD_Y * 2;
        int boxX = rightEdge - boxW;
        int boxY = 0;

        if (WeaponConfig.showTextPanel) {
            // Flat fill, SAME box dimensions the border below uses - a glow here previously spread
            // visually wider than the tightly-fit box the border/brackets were drawn against, which
            // read as the two simply not matching each other at all. Sharing one box fixes that.
            HudPanel.draw(g, boxX, boxY, boxW, boxH, WeaponConfig.textPanelColor, WeaponConfig.textPanelOpacity);
        }
        if (WeaponConfig.showPanel) {
            HudPanel.drawBorder(g, boxX, boxY, boxW, boxH, WeaponConfig.borderColor,
                    WeaponConfig.borderOpacity, WeaponConfig.borderStyle, WeaponConfig.borderLength, WeaponConfig.borderOutset);
        }
        if (low && WeaponConfig.showWeaponGlow) {
            float pulseAlpha = 0.20f + 0.28f * 0.5f * (1f + (float) Math.sin(System.currentTimeMillis() / 260.0));
            GlowEffect.drawGlowRect(g, boxX, boxY, boxW, boxH, WeaponConfig.ammoLowColor, pulseAlpha, 3f);
        }

        int cursorY = boxY + CHIP_PAD_Y;
        float rowRightX = rightEdge - CHIP_PAD_X;
        float cursorX = rowRightX;

        if (hasReserve && WeaponConfig.showAmmoTypeIcon) {
            cursorX -= iconW;
            int iconY = cursorY + (ammoRowH - iconW) / 2;
            drawAmmoTypeIcon(g, Math.round(cursorX), iconY, iconW, WeaponConfig.reserveColor,
                    (float) (WeaponConfig.reserveOpacity / 100.0));
            cursorX -= iconGap;
        }
        if (hasReserve) {
            int reserveTextH = Math.round(font.lineHeight * reserveScale);
            float reserveY = cursorY + (ammoRowH - reserveTextH) / 2f;
            cursorX -= reserveW;
            drawZeroPaddedNumber(g, font, reserveText, cursorX, reserveY, reserveScale, reserveColor);
            cursorX -= sepW;
            drawScaledText(g, font, sep, cursorX, reserveY, reserveScale, reserveColor);
        }
        cursorX -= magW;
        drawZeroPaddedNumber(g, font, magText, cursorX, cursorY, ammoScale, magColor);

        return boxY + boxH;
    }

    /** The weapon's name — plain floating text, no background of its own, matching the reference
     *  HUD (the ammo row's glow does not extend down over this). Always visible, no fade. */
    private int drawNameRow(GuiGraphics g, Font font, ItemStack gun, int rightEdge, int y) {
        if (!WeaponConfig.showName) {
            return y;
        }
        String name = gun.getHoverName().getString();
        float nameW = font.width(name);
        int nameColor = WeaponConfig.nameColor | 0xFF000000;
        g.drawString(font, name, Math.round(rightEdge - CHIP_PAD_X - nameW), y, nameColor, true);
        return y + font.lineHeight;
    }

    /**
     * Fire mode as a segmented control: ONE box divided into equal-width cells, one per supported
     * mode, each showing that mode's icon — matching the reference HUD's 2-cell split button rather
     * than a loose row of icons. The ACTIVE mode's cell gets an extra highlight border drawn around
     * just that cell ("whichever mode is active, THAT one gets framed"), on top of the shared outer
     * box/border every other chip uses. Hidden entirely for guns with one (or zero reported) modes -
     * nothing to indicate. Falls back to a single-letter label per cell if the icon-set lookup comes
     * back empty for some reason, so real multi-mode support never silently shows nothing.
     */
    private void drawFireModeSegmented(GuiGraphics g, Font font, ItemStack gun, int rightEdge, int y) {
        if (!WeaponConfig.showFireMode) {
            return;
        }
        String[] modes = TaczCompat.supportedFireModes(gun);
        if (modes.length <= 1) {
            return;
        }
        String activeMode = TaczCompat.fireMode(gun);

        int cellW = FIRE_ICON_SIZE + FIRE_ICON_GAP;
        int cellH = FIRE_ICON_SIZE + CHIP_PAD_Y * 2;
        int boxW = cellW * modes.length;
        int boxX = rightEdge - boxW;

        // Flat fill only, deliberately NO corner-bracket/border accent on the OUTER box - that's
        // meant for the icon boxes and the ammo row, not this segmented control. The active cell's
        // own highlight border below is the only border-like accent this element gets.
        if (WeaponConfig.showPanel) {
            HudPanel.draw(g, boxX, y, boxW, cellH, WeaponConfig.panelColor, WeaponConfig.panelOpacity);
        }

        for (int i = 0; i < modes.length; i++) {
            String mode = modes[i];
            int cellX = boxX + i * cellW;
            boolean isActive = mode.equals(activeMode);

            if (i > 0 && WeaponConfig.showPanel) {
                // A thin divider between cells - the outer box's own border already frames all four
                // outside edges, this just separates the cells from each other internally.
                int dividerAlpha = Math.max(1, (int) Math.round(WeaponConfig.borderOpacity * 0.6 * 2.55));
                int dividerColor = (dividerAlpha << 24) | (WeaponConfig.borderColor & 0xFFFFFF);
                g.fill(cellX, y, cellX + 1, y + cellH, dividerColor);
            }
            if (isActive && WeaponConfig.showPanel) {
                HudPanel.drawFullBorder(g, cellX, y, cellW, cellH, WeaponConfig.borderColor, WeaponConfig.borderOpacity);
            }

            ResourceLocation icon = iconFor(mode);
            int iconX = cellX + (cellW - FIRE_ICON_SIZE) / 2;
            int iconY = y + (cellH - FIRE_ICON_SIZE) / 2;
            if (icon != null) {
                RenderSystem.enableBlend();
                try {
                    if (isActive) {
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    } else {
                        RenderSystem.setShaderColor(0.55f, 0.55f, 0.55f, 0.55f);
                    }
                    g.blit(icon, iconX, iconY, 0, 0, FIRE_ICON_SIZE, FIRE_ICON_SIZE, FIRE_ICON_SIZE, FIRE_ICON_SIZE);
                } finally {
                    // GUARANTEED restore even if blit() throws (a missing/corrupt texture from a
                    // resource-pack conflict, for instance) - otherwise the dimmed colour set above
                    // leaks into every OTHER draw call for the rest of the frame, which is exactly
                    // the kind of bug that made the whole shadowbox appear to "vanish" once before.
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    RenderSystem.disableBlend();
                }
            } else {
                String label = mode.isEmpty() ? "?" : mode.substring(0, 1);
                int fmColor = (isActive ? 0xFFFFFFFF : (WeaponConfig.fireModeColor | 0xFF000000));
                g.drawString(font, label, cellX + (cellW - font.width(label)) / 2,
                        y + (cellH - font.lineHeight) / 2, fmColor, true);
            }
        }
    }

    /** Splits a (possibly zero-padded) number into a dim leading-zero portion and a bright
     *  significant-digit portion, drawing each separately - e.g. "030" draws a faint "0" then a
     *  bright "30". A number with no leading zeros (or zero-padding turned off, so nothing to dim)
     *  just draws normally in one piece. An all-zero string ("000") treats everything but the LAST
     *  digit as the dim "leading" portion, since the final digit is still the real value shown. */
    private void drawZeroPaddedNumber(GuiGraphics g, Font font, String text, float x, float y, float scale, int color) {
        if (!WeaponConfig.zeroPadAmmo || text.length() <= 1) {
            drawScaledText(g, font, text, x, y, scale, color);
            return;
        }
        int splitIndex = 0;
        while (splitIndex < text.length() - 1 && text.charAt(splitIndex) == '0') {
            splitIndex++;
        }
        if (splitIndex == 0) {
            drawScaledText(g, font, text, x, y, scale, color);
            return;
        }
        String dimPart = text.substring(0, splitIndex);
        String brightPart = text.substring(splitIndex);
        int alpha = (color >>> 24) & 0xFF;
        int dimAlpha = Math.max(1, Math.round(alpha * 0.35f));
        int dimColor = (dimAlpha << 24) | (color & 0xFFFFFF);
        drawScaledText(g, font, dimPart, x, y, scale, dimColor);
        drawScaledText(g, font, brightPart, x + font.width(dimPart) * scale, y, scale, color);
    }

    /** The "ammo type" icon next to reserve — reuses the same full-auto fire-mode texture already
     *  used elsewhere on this widget (see {@link WeaponConfig#showAmmoTypeIcon}), tinted to match
     *  the reserve number, rather than a crude programmatically-drawn 3-line mark. Sized to {@code
     *  size} (the caller matches this to the ammo digits' own rendered height - see
     *  {@link #drawAmmoRow}) rather than a fixed pixel size.
     *
     * <p>Unlike the fire-mode row's own icons (drawn at a fixed {@link #FIRE_ICON_SIZE} straight off
     * the source PNG with no real crop), this draws a tight, SQUARE crop of just the icon's actual
     * content ({@link #FIRE_ICON_CROP_U}/{@code _V}/{@code _W}/{@code _H}, measured against the real
     * {@link #FIRE_ICON_TEX_SIZE}x{@code FIRE_ICON_TEX_SIZE} source) at an independent destination
     * size, using the same crop-with-independent-scale blit already used for the player head render
     * (see {@link PlayerHudOverlay#renderHead}). The source PNG has a lot of transparent padding
     * around the actual icon - stretching that whole padded canvas down to a small box (the earlier
     * approach here) spent most of the destination pixels on blank space, which read as
     * mushy/warped once this icon was sized up to match the ammo digits. Cropping to just the real
     * content first, THEN scaling, keeps it clean at any size. The crop is deliberately square so
     * scaling it onto this method's square destination box never skews the aspect ratio either. */
    private void drawAmmoTypeIcon(GuiGraphics g, int x, int y, int size, int colorRgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        if (a <= 0 || size <= 0) {
            return;
        }
        float r = ((colorRgb >> 16) & 0xFF) / 255f;
        float gr = ((colorRgb >> 8) & 0xFF) / 255f;
        float b = (colorRgb & 0xFF) / 255f;
        RenderSystem.enableBlend();
        try {
            RenderSystem.setShaderColor(r, gr, b, alpha);
            g.blit(FIRE_ICON_AUTO, x, y, size, size,
                    FIRE_ICON_CROP_U, FIRE_ICON_CROP_V, FIRE_ICON_CROP_W, FIRE_ICON_CROP_H,
                    FIRE_ICON_TEX_SIZE, FIRE_ICON_TEX_SIZE);
        } finally {
            // Same guaranteed-restore reasoning as every other tinted blit in this class - a
            // missing/corrupt texture throwing here must never leave the tint stuck globally.
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }

    /** Draws {@code text} with its top-left corner at the given SCREEN-space position, at its own
     *  independent scale — used where two pieces of text on the same row need different scales
     *  (the mag number and the smaller reserve number), so each can be pushed/scaled/popped on its
     *  own without disturbing the other's coordinate space. */
    private void drawScaledText(GuiGraphics g, Font font, String text, float screenX, float screenY, float scale, int color) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(screenX, screenY, 0);
        pose.scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, color, true);
        pose.popPose();
    }

    /** Reserve count for a gun that's ACTIVELY held/fired — no zero-padding. Empty string means
     *  "nothing to show" (inventory-ammo guns, which have no separate reserve concept); the "|"
     *  separator itself is drawn by the caller, not included here, so an empty reserve cleanly
     *  collapses the whole "| reserve" portion rather than leaving a stray separator behind. */
    private String reserveString(LocalPlayer player, ItemStack gun) {
        int reserve = TaczCompat.reserveAmmo(player, gun);
        if (reserve == TaczCompat.INFINITE) {
            return "\u221E";
        }
        if (reserve == TaczCompat.NO_RESERVE) {
            return "";
        }
        return ammoString(reserve);
    }

    private String ammoString(int count) {
        return WeaponConfig.zeroPadAmmo ? String.format("%03d", count) : String.valueOf(count);
    }

    private ResourceLocation iconFor(String mode) {
        return switch (mode) {
            case "SEMI" -> FIRE_ICON_SEMI;
            case "AUTO" -> FIRE_ICON_AUTO;
            case "BURST" -> FIRE_ICON_BURST;
            default -> null;
        };
    }

    /**
     * "FIRE TYPE: FULL AUTO" (or just "FULL AUTO" if {@link WeaponConfig#fireTypeShowLabel} is off)
     * — flashes centre-screen, near where the ammo readout/alert text sits by default (see
     * {@link WeaponConfig#fireTypeXOffset}/{@link WeaponConfig#fireTypeYOffset} — same "positive =
     * right/down" sign convention as {@code ammoAlertXOffset}/{@code ammoAlertYOffset}, so the two
     * can be lined up together): starts in the flash colour, switches to the hold colour once the
     * flash phase ends, then fades out completely. The mode name itself is underlined; the
     * "FIRE TYPE: " prefix (when shown) isn't. Replays on EITHER of two triggers — switching to a
     * gun ({@link #switchAnimStartMs}) or cycling fire mode on the gun you're already holding
     * ({@link #fireModeChangeStartMs}) — driven by whichever one most recently fired, so it plays
     * exactly once per event, not on every frame the gun stays out. Silently does nothing for a gun
     * with no fire-mode data at all.
     */
    private void drawFireTypeFlash(GuiGraphics g, Font font, ItemStack gun, int screenWidth, int screenHeight) {
        String rawMode = TaczCompat.fireMode(gun);
        if (rawMode == null || rawMode.isEmpty()) {
            return;
        }
        long flashMs = Math.max(0, WeaponConfig.fireTypeFlashMs);
        long holdMs = Math.max(0, WeaponConfig.fireTypeHoldMs);
        long fadeMs = Math.max(1, WeaponConfig.fireTypeFadeMs);
        long totalMs = flashMs + holdMs + fadeMs;
        long elapsed = Util.getMillis() - Math.max(switchAnimStartMs, fireModeChangeStartMs);
        if (elapsed < 0 || elapsed >= totalMs) {
            return;
        }

        int color;
        float alpha;
        if (elapsed < flashMs) {
            color = WeaponConfig.fireTypeFlashColor;
            alpha = 1.0f;
        } else if (elapsed < flashMs + holdMs) {
            color = WeaponConfig.fireTypeHoldColor;
            alpha = 1.0f;
        } else {
            color = WeaponConfig.fireTypeHoldColor;
            alpha = 1.0f - (float) (elapsed - flashMs - holdMs) / fadeMs;
        }
        int a = Math.max(1, Math.round(alpha * 255f));
        int argb = (a << 24) | (color & 0xFFFFFF);

        String modeLabel = fireTypeLabel(rawMode);
        // Decade: the words come from the language files (they were English literals)
        String prefix = WeaponConfig.fireTypeShowLabel ? net.minecraft.client.resources.language.I18n.get("modernhud.hud.fire_type") : "";

        float scale = (float) WeaponConfig.fireTypeScale;
        int prefixW = font.width(prefix);
        int modeW = font.width(modeLabel);
        int totalW = Math.round((prefixW + modeW) * scale);
        int textH = Math.round(font.lineHeight * scale);

        // Same "positive = right/down from screen centre" convention as the ammo alert overlay's
        // offsets, and (unlike before) NEGATIVE is fully supported on both axes - this used to only
        // be able to move UP from centre, which meant it could never be tucked down near the ammo
        // readout/alert area at all.
        int x = screenWidth / 2 - totalW / 2 + WeaponConfig.fireTypeXOffset;
        int y = screenHeight / 2 - textH / 2 + WeaponConfig.fireTypeYOffset;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        if (!prefix.isEmpty()) {
            g.drawString(font, prefix, 0, 0, argb, true);
        }
        g.drawString(font, modeLabel, prefixW, 0, argb, true);
        g.fill(prefixW, font.lineHeight, prefixW + modeW, font.lineHeight + 1, argb); // underline the mode only
        pose.popPose();
    }

    /** Raw TACZ fire-mode id -&gt; the label shown in the flash announcement. */
    private String fireTypeLabel(String rawMode) {
        return switch (rawMode) {
            case "SEMI" -> net.minecraft.client.resources.language.I18n.get("modernhud.hud.fire_mode.semi");
            case "AUTO" -> net.minecraft.client.resources.language.I18n.get("modernhud.hud.fire_mode.auto");
            case "BURST" -> net.minecraft.client.resources.language.I18n.get("modernhud.hud.fire_mode.burst");
            default -> rawMode;
        };
    }

    /**
     * A row of small gold squares INSIDE the primary icon box's own top-RIGHT corner, one per
     * EQUIPPED attachment (via {@link TaczCompat#attachmentSlotCounts}) — an empty or unsupported
     * slot draws nothing at all, not even a dim placeholder, so the row's width simply reflects how
     * many attachments are actually on the gun right now, and a bare gun draws none. Drawn AFTER
     * (on top of) the icon box itself, inset from its edges, rather than as a separate row floating
     * above it.
     */
    private void drawAttachmentDots(GuiGraphics g, ItemStack gun, int boxX, int boxY, int boxW) {
        int[] counts = TaczCompat.attachmentSlotCounts(gun);
        int filled = counts[1];
        if (filled <= 0) {
            return;
        }
        int inset = 3;
        int dotsW = filled * ATTACHMENT_DOT_SIZE + (filled - 1) * ATTACHMENT_DOT_GAP;
        int maxW = boxW - inset * 2;
        if (dotsW > maxW) {
            dotsW = maxW; // clamp - shouldn't realistically happen (max 6 slots), but stay inside the box
        }
        int x = boxX + boxW - inset - dotsW; // right-aligned within the box, not centred
        int y = boxY + inset;
        int color = 0xFF000000 | (WeaponConfig.attachmentDotColor & 0xFFFFFF);
        for (int i = 0; i < filled; i++) {
            g.fill(x, y, x + ATTACHMENT_DOT_SIZE, y + ATTACHMENT_DOT_SIZE, color);
            x += ATTACHMENT_DOT_SIZE + ATTACHMENT_DOT_GAP;
        }
    }

    /** The primary weapon's icon, centred in its fixed box, with an optional flat background chip
     *  (+ corner brackets) behind it, drawn through {@link #drawChipBox} like every other element. */
    private void drawPrimaryIcon(GuiGraphics g, ItemStack gun, int boxX, int boxY, int boxW, int boxH,
                                  float switchGlowAlpha, float switchBorderFlashAlpha) {
        drawChipBox(g, boxX, boxY, boxW, boxH, 1.0);
        if (switchBorderFlashAlpha > 0.01f && WeaponConfig.showPanel) {
            // An EXTRA border pass in the flash colour, layered on top of the normal one at fading
            // opacity - as less of this shows through, the normal border underneath dominates again,
            // which reads as "gold fading back to normal" without needing to interpolate the colours.
            HudPanel.drawBorder(g, boxX, boxY, boxW, boxH, WeaponConfig.switchBorderFlashColor,
                    WeaponConfig.borderOpacity * switchBorderFlashAlpha, WeaponConfig.borderStyle, WeaponConfig.borderLength, WeaponConfig.borderOutset);
        }
        if (TaczCompat.hasHudTexture(gun)) {
            int[] size = TaczCompat.iconSize((float) WeaponConfig.iconScale);
            int ix = boxX + (boxW - size[0]) / 2;
            int iy = boxY + (boxH - size[1]) / 2;
            TaczCompat.renderHudTexture(g, gun, ix, iy, (float) WeaponConfig.iconScale);
        } else {
            int size = Math.min(boxW, boxH) - 8;
            g.renderItem(gun, boxX + (boxW - size) / 2, boxY + (boxH - size) / 2);
        }
        if (switchGlowAlpha > 0.01f) {
            // Drawn LAST (on top of the icon) so it reads as a bright flash the moment the icon
            // settles into place, not a glow occluded behind it.
            GlowEffect.drawGlowRect(g, boxX, boxY, boxW, boxH, WeaponConfig.weaponGlowColor, switchGlowAlpha, 3f);
        }
    }

    /**
     * The secondary weapon: a small, dim icon directly BELOW the primary icon box - its own strip,
     * entirely clear of the ammo/name/fire-mode text column, so it can never overlap them regardless
     * of frame size. Looks the gun's ResourceLocation up as a LIVE ItemStack in the hotbar every
     * frame (rather than caching the ItemStack itself), so ammo/skin state is never stale and a gun
     * that's been dropped/consumed/moved out of the hotbar since simply stops showing instead of
     * leaving a ghost behind.
     */
    private void drawSecondaryWeapon(GuiGraphics g, LocalPlayer player, int iconBoxX, int stripY) {
        if (secondaryGunId == null) {
            return;
        }
        ItemStack secondaryStack = findInHotbar(player.getInventory(), secondaryGunId);
        if (secondaryStack == null) {
            secondaryGunId = null; // no longer carried - forget it rather than show a ghost
            return;
        }
        drawCarriedIcon(g, player, secondaryStack, iconBoxX, stripY, WeaponConfig.secondaryAlpha);
    }

    /** One entry of the neutral-state pair — identical treatment to the normal secondary slot (same
     *  box, same dim alpha, same icon scale, same reserve readout), just driven by hotbar order
     *  instead of switch-history. */
    private void drawNeutralIcon(GuiGraphics g, LocalPlayer player, ResourceLocation gunId, int boxX, int boxY) {
        ItemStack stack = findInHotbar(player.getInventory(), gunId);
        if (stack == null) {
            return;
        }
        drawCarriedIcon(g, player, stack, boxX, boxY, WeaponConfig.secondaryAlpha);
    }

    /**
     * Icon + reserve-ammo readout for a gun that ISN'T actively being held/fired right now — the
     * shared drawing behind BOTH {@link #drawSecondaryWeapon} and {@link #drawNeutralIcon}: same
     * box, same dim treatment, same reserve-count text, just a different gun-selection source (
     * switch-history vs. hotbar order). Mirrors the reference HUD's secondary-weapon slot exactly:
     * the box is the SAME WIDTH as the primary icon box (not its own smaller width), the icon sits
     * right-aligned/toward the inside of that box, and the reserve ammo count fills the space that
     * leaves on the left — one box holding both, not the icon alone with the count elsewhere.
     */
    private void drawCarriedIcon(GuiGraphics g, LocalPlayer player, ItemStack stack, int boxX, int boxY, double alphaMul) {
        int boxW = ICON_BOX_W;
        int boxH = SECONDARY_BOX_H;

        drawSecondaryChipBox(g, boxX, boxY, boxW, boxH, alphaMul);

        float[] prevColor = RenderSystem.getShaderColor();
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float) alphaMul);
            if (TaczCompat.hasHudTexture(stack)) {
                int[] size = TaczCompat.iconSize((float) WeaponConfig.secondaryIconScale);
                int ix = boxX + boxW - size[0] - 2; // right-aligned/inward, small inset from the box edge
                int iy = boxY + (boxH - size[1]) / 2;
                TaczCompat.renderHudTexture(g, stack, ix, iy, (float) WeaponConfig.secondaryIconScale);
            } else {
                int size = Math.min(boxH - 4, 20);
                g.renderItem(stack, boxX + boxW - size - 2, boxY + (boxH - size) / 2);
            }
        } finally {
            // Same guaranteed-restore reasoning as the fire-mode icon loop above - a missing/corrupt
            // gun HUD icon texture throwing here must never leave the dimmed alpha stuck globally.
            RenderSystem.setShaderColor(prevColor[0], prevColor[1], prevColor[2], prevColor[3]);
        }

        if (WeaponConfig.showReserveAmmo) {
            int reserve = TaczCompat.reserveAmmo(player, stack);
            String text = reserve == TaczCompat.INFINITE ? "\u221E"
                    : reserve == TaczCompat.NO_RESERVE ? "" : String.valueOf(reserve);
            if (!text.isEmpty()) {
                Font font = Minecraft.getInstance().font;
                int tx = boxX + CHIP_PAD_X; // left-aligned - the space the right-aligned icon leaves free
                int ty = boxY + (boxH - font.lineHeight) / 2;
                int a = Math.max(1, (int) Math.round(alphaMul * 255));
                int color = (a << 24) | (WeaponConfig.reserveColor & 0xFFFFFF);
                g.drawString(font, text, tx, ty, color, true);
            }
        }
    }

    private ItemStack findInHotbar(Inventory inventory, ResourceLocation gunId) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && gunId.equals(TaczCompat.gunId(stack))) {
                return stack;
            }
        }
        return null;
    }

    private int[] resolvePosition(int screenWidth, int screenHeight, int width, int height, double scale) {
        int scaledWidth = (int) Math.round(width * scale);
        int scaledHeight = (int) Math.round(height * scale);
        int x;
        int y;
        switch (WeaponConfig.position) {
            case TOP_LEFT -> { x = WeaponConfig.offsetX; y = WeaponConfig.offsetY; }
            case TOP_RIGHT -> { x = screenWidth - scaledWidth - WeaponConfig.offsetX; y = WeaponConfig.offsetY; }
            case BOTTOM_LEFT -> { x = WeaponConfig.offsetX; y = screenHeight - scaledHeight - WeaponConfig.offsetY; }
            case CUSTOM -> { x = WeaponConfig.offsetX; y = WeaponConfig.offsetY; }
            default -> { x = screenWidth - scaledWidth - WeaponConfig.offsetX; y = screenHeight - scaledHeight - WeaponConfig.offsetY; }
        }
        return new int[]{x, y};
    }
}
