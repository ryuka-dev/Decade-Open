package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client-only configuration for the TACZ weapon widget (bottom-right by default): the held gun's
 * icon, name, fire mode, and mag/reserve ammo — drawn in the same clean, flat "fade box + corner
 * accent lines" chrome as the player widget (see {@link HudConfig}'s panel section and
 * {@code HudPanel}), with no drop shadow and no ammo gauge bar under the numbers — numbers only.
 * Entirely inert (never registers its overlay) when TACZ isn't installed.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class WeaponConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.EnumValue<WeaponHudSource> PREFERRED_HUD_SOURCE;
    private static final ForgeConfigSpec.BooleanValue SHOW_NAME;
    private static final ForgeConfigSpec.BooleanValue SHOW_FIRE_MODE;
    private static final ForgeConfigSpec.BooleanValue SHOW_RESERVE_AMMO;

    private static final ForgeConfigSpec.EnumValue<HudPosition> POSITION;
    private static final ForgeConfigSpec.IntValue OFFSET_X;
    private static final ForgeConfigSpec.IntValue OFFSET_Y;
    private static final ForgeConfigSpec.DoubleValue SCALE;

    private static final ForgeConfigSpec.DoubleValue ICON_SCALE;
    private static final ForgeConfigSpec.DoubleValue AMMO_TEXT_SCALE;
    private static final ForgeConfigSpec.IntValue FRAME_WIDTH;
    private static final ForgeConfigSpec.IntValue FRAME_HEIGHT;
    private static final ForgeConfigSpec.BooleanValue ZERO_PAD_AMMO;

    private static final ForgeConfigSpec.BooleanValue SHOW_SECONDARY;
    private static final ForgeConfigSpec.DoubleValue SECONDARY_ICON_SCALE;
    private static final ForgeConfigSpec.DoubleValue SECONDARY_ALPHA;


    private static final ForgeConfigSpec.BooleanValue SHOW_PANEL;
    private static final ForgeConfigSpec.ConfigValue<String> PANEL_COLOR;
    private static final ForgeConfigSpec.IntValue PANEL_OPACITY;
    private static final ForgeConfigSpec.EnumValue<PanelBorderStyle> BORDER_STYLE;
    private static final ForgeConfigSpec.ConfigValue<String> BORDER_COLOR;
    private static final ForgeConfigSpec.IntValue BORDER_OPACITY;
    private static final ForgeConfigSpec.IntValue BORDER_LENGTH;
    private static final ForgeConfigSpec.IntValue BORDER_OUTSET;
    private static final ForgeConfigSpec.BooleanValue SHOW_WEAPON_GLOW;
    private static final ForgeConfigSpec.ConfigValue<String> WEAPON_GLOW_COLOR;
    private static final ForgeConfigSpec.BooleanValue SHOW_TEXT_PANEL;
    private static final ForgeConfigSpec.ConfigValue<String> TEXT_PANEL_COLOR;
    private static final ForgeConfigSpec.IntValue TEXT_PANEL_OPACITY;
    private static final ForgeConfigSpec.BooleanValue SHOW_AMMO_TYPE_ICON;
    private static final ForgeConfigSpec.DoubleValue AMMO_TYPE_ICON_SCALE;
    private static final ForgeConfigSpec.BooleanValue SHOW_ATTACHMENT_DOTS;
    private static final ForgeConfigSpec.ConfigValue<String> ATTACHMENT_DOT_COLOR;
    private static final ForgeConfigSpec.IntValue SECONDARY_DOT_SIZE;
    private static final ForgeConfigSpec.BooleanValue SHOW_SWITCH_SLIDE;
    private static final ForgeConfigSpec.IntValue SWITCH_SLIDE_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue SHOW_SWITCH_BORDER_FLASH;
    private static final ForgeConfigSpec.ConfigValue<String> SWITCH_BORDER_FLASH_COLOR;
    private static final ForgeConfigSpec.IntValue SWITCH_BORDER_FLASH_MS;

    private static final ForgeConfigSpec.ConfigValue<String> AMMO_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> AMMO_LOW_COLOR;
    private static final ForgeConfigSpec.DoubleValue LOW_AMMO_THRESHOLD_PERCENT;
    private static final ForgeConfigSpec.ConfigValue<String> RESERVE_COLOR;
    private static final ForgeConfigSpec.DoubleValue RESERVE_SCALE_OFFSET;
    private static final ForgeConfigSpec.IntValue RESERVE_OPACITY;
    private static final ForgeConfigSpec.ConfigValue<String> NAME_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> FIRE_MODE_COLOR;

    private static final ForgeConfigSpec.BooleanValue SHOW_FIRE_TYPE_FLASH;
    private static final ForgeConfigSpec.ConfigValue<String> FIRE_TYPE_FLASH_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> FIRE_TYPE_HOLD_COLOR;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_FLASH_MS;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_HOLD_MS;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_FADE_MS;
    private static final ForgeConfigSpec.DoubleValue FIRE_TYPE_SCALE;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_X_OFFSET;
    private static final ForgeConfigSpec.IntValue FIRE_TYPE_Y_OFFSET;
    private static final ForgeConfigSpec.BooleanValue FIRE_TYPE_SHOW_LABEL;

    private static final ForgeConfigSpec.BooleanValue SHOW_AMMO_ALERTS;
    private static final ForgeConfigSpec.ConfigValue<String> RELOAD_ALERT_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> NO_AMMO_ALERT_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> LOW_AMMO_ALERT_COLOR;
    private static final ForgeConfigSpec.DoubleValue AMMO_ALERT_SCALE;
    private static final ForgeConfigSpec.IntValue AMMO_ALERT_Y_OFFSET;

    public static final ForgeConfigSpec SPEC;

    /** Derived from {@link #preferredHudSource} — true only when that's MODERNHUD. Kept as a plain
     *  field (not its own config value) so every existing "is our widget on" check elsewhere in the
     *  codebase keeps working unchanged. */
    public static boolean enabled;
    public static WeaponHudSource preferredHudSource;
    public static boolean showName;
    public static boolean showFireMode;
    public static boolean showReserveAmmo;

    public static HudPosition position;
    public static int offsetX;
    public static int offsetY;
    public static double scale;

    public static double iconScale;
    public static double ammoTextScale;
    public static int frameWidth;
    public static int frameHeight;
    public static boolean zeroPadAmmo;

    public static boolean showSecondary;
    public static double secondaryIconScale;
    public static double secondaryAlpha;


    public static boolean showPanel;
    public static int panelColor;
    public static double panelOpacity;
    public static PanelBorderStyle borderStyle;
    public static int borderColor;
    public static double borderOpacity;
    public static int borderLength;
    public static int borderOutset;
    public static boolean showWeaponGlow;
    public static int weaponGlowColor;
    public static boolean showTextPanel;
    public static int textPanelColor;
    public static double textPanelOpacity;
    public static boolean showAmmoTypeIcon;
    public static double ammoTypeIconScale;
    public static boolean showAttachmentDots;
    public static int attachmentDotColor;
    public static int secondaryDotSize;
    public static boolean showSwitchSlide;
    public static int switchSlideDistance;
    public static boolean showSwitchBorderFlash;
    public static int switchBorderFlashColor;
    public static int switchBorderFlashMs;

    public static int ammoColor;
    public static int ammoLowColor;
    public static double lowAmmoThresholdPercent;
    public static int reserveColor;
    public static double reserveScaleOffset;
    public static double reserveOpacity;
    public static int nameColor;
    public static int fireModeColor;

    public static boolean showFireTypeFlash;
    public static int fireTypeFlashColor;
    public static int fireTypeHoldColor;
    public static int fireTypeFlashMs;
    public static int fireTypeHoldMs;
    public static int fireTypeFadeMs;
    public static double fireTypeScale;
    public static int fireTypeXOffset;
    public static int fireTypeYOffset;
    public static boolean fireTypeShowLabel;

    public static boolean showAmmoAlerts;
    public static int reloadAlertColor;
    public static int noAmmoAlertColor;
    public static int lowAmmoAlertColor;
    public static double ammoAlertScale;
    public static int ammoAlertYOffset;

    static {
        BUILDER.comment("TACZ weapon widget - only ever shows/registers when the TACZ mod is installed.",
                "Ammo is shown as plain numbers only (mag / reserve) - deliberately no ammo gauge",
                "bar under them, and no drop-shadow box behind the panel, just the one flat fade panel.")
                .push("general");

        PREFERRED_HUD_SOURCE = BUILDER
                .comment("Which weapon HUD should actually be on screen: TACZ_HUD (TACZ's own",
                        "built-in one), TACZ_P_HUD (TaczPresence's, if that mod is installed -",
                        "falls back to TACZ_HUD if it isn't), or MODERNHUD (this mod's widget, the",
                        "default). Whichever you pick, ModernHUD makes sure only that one actually",
                        "renders - including syncing TaczPresence's own equivalent selector when",
                        "that mod is present, so the two mods never fight over which HUD shows.")
                .translation("modernhud.config.weapon_hud_source")
                .defineEnum("hudSource", WeaponHudSource.MODERNHUD);

        SHOW_NAME = BUILDER
                .comment("Show the held gun's name, always-on (no fade), directly under the ammo",
                        "numbers inside the same combined chip - matching the reference HUD layout.")
                .translation("modernhud.config.weapon_show_name")
                .define("showName", true);

        SHOW_FIRE_MODE = BUILDER
                .comment("Show the current fire mode (SEMI / AUTO / BURST, as TACZ reports it).")
                .translation("modernhud.config.weapon_show_fire_mode")
                .define("showFireMode", true);

        SHOW_RESERVE_AMMO = BUILDER
                .comment("Show reserve ammo (inventory total) next to the mag count, as 'mag / reserve'.",
                        "Turned off entirely for guns that use inventory ammo directly (no separate reserve).")
                .translation("modernhud.config.weapon_show_reserve")
                .define("showReserveAmmo", true);

        BUILDER.pop();

        BUILDER.comment("Where the weapon widget is anchored on screen").push("position");

        POSITION = BUILDER
                .comment("Screen corner the widget is anchored to. Use CUSTOM to position it freely with offsetX/offsetY.",
                        "Allowed values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM")
                .translation("modernhud.config.weapon_position")
                .defineEnum("anchor", HudPosition.BOTTOM_RIGHT);

        OFFSET_X = BUILDER
                .comment("Horizontal offset in pixels from the chosen anchor (or absolute X when anchor = CUSTOM).")
                .translation("modernhud.config.weapon_offset_x")
                .defineInRange("offsetX", 8, -10000, 10000);

        OFFSET_Y = BUILDER
                .comment("Vertical offset in pixels from the chosen anchor (or absolute Y when anchor = CUSTOM).")
                .translation("modernhud.config.weapon_offset_y")
                .defineInRange("offsetY", 8, -10000, 10000);

        SCALE = BUILDER
                .comment("Overall scale multiplier applied to the whole weapon widget.")
                .translation("modernhud.config.weapon_scale")
                .defineInRange("scale", 1.0, 0.1, 5.0);

        BUILDER.pop();

        BUILDER.comment("Icon scale, and the FIXED size of the whole widget's content box.",
                "Fixed on purpose (TaczPresence style): the panel never resizes per gun. TACZ HUD-icon",
                "art is always drawn from a fixed 39x13 canvas at this uniform scale (never stretched",
                "or read from the PNG's own pixel size - that's what caused distorted icons before),",
                "and text is pinned to fixed corners inside the panel, never used to grow it.")
                .push("appearance");

        ICON_SCALE = BUILDER
                .comment("Uniform scale applied to TACZ's fixed 39x13 HUD-icon canvas (e.g. 1.5 -> drawn at 58x19).")
                .translation("modernhud.config.weapon_icon_scale")
                .defineInRange("iconScale", 1.5, 0.25, 6.0);

        AMMO_TEXT_SCALE = BUILDER
                .comment("Scale of the big mag-ammo number specifically (independent of the overall widget scale).")
                .translation("modernhud.config.weapon_ammo_text_scale")
                .defineInRange("ammoTextScale", 1.5, 0.5, 4.0);

        FRAME_WIDTH = BUILDER
                .comment("Fixed width in px (before scale) of the whole widget's bounding box - used for",
                        "screen-edge anchoring (see 'position' above). Same for every gun; long weapon",
                        "names never grow this, they just render inside the combined ammo/name/fire-mode",
                        "chip (see the 'colors' section for its text colour). The default comfortably",
                        "fits the icon, mag|reserve numbers, name, and fire-mode row at their default",
                        "sizes - shrink it only if you've also reduced iconScale/ammoTextScale to match.")
                .translation("modernhud.config.weapon_panel_width")
                .defineInRange("frameWidth", 160, 32, 1000);

        FRAME_HEIGHT = BUILDER
                .comment("Fixed height in px (before scale) of the whole widget's bounding box - used for",
                        "screen-edge anchoring. Needs to be tall enough for the icon + secondary weapon",
                        "slot stacked underneath it; the default already accounts for both at their",
                        "default sizes. Attachment dots draw INSIDE the icon box's own top edge, so they",
                        "don't need extra height reserved for them.")
                .translation("modernhud.config.weapon_panel_height")
                .defineInRange("frameHeight", 60, 16, 1000);

        ZERO_PAD_AMMO = BUILDER
                .comment("Zero-pad the mag/reserve ammo numbers to 3 digits (e.g. '030' instead of '30') -",
                        "the classic Modern Warfare HUD look.")
                .translation("modernhud.config.weapon_zero_pad_ammo")
                .define("zeroPadAmmo", true);

        BUILDER.pop();

        BUILDER.comment("The secondary weapon slot: a small, dim icon-only thumbnail for the last",
                "DIFFERENT gun you were holding before your current one (as long as it's still",
                "somewhere in your hotbar) - Call of Duty's primary/secondary weapon HUD, basically.",
                "Whichever gun you're actually holding is always the big primary display; switch back",
                "to the other one and they simply swap roles.")
                .push("secondary");

        SHOW_SECONDARY = BUILDER
                .comment("Show the secondary weapon thumbnail.")
                .translation("modernhud.config.weapon_show_secondary")
                .define("showSecondary", true);

        SECONDARY_ICON_SCALE = BUILDER
                .comment("Uniform scale applied to the secondary weapon's fixed 39x13 HUD-icon canvas.",
                        "Kept smaller than the primary iconScale so it reads as clearly secondary.")
                .translation("modernhud.config.weapon_secondary_icon_scale")
                .defineInRange("secondaryIconScale", 0.85, 0.15, 4.0);

        SECONDARY_ALPHA = BUILDER
                .comment("Opacity (0-1) of the secondary weapon thumbnail, so it visually recedes",
                        "behind the primary weapon.")
                .translation("modernhud.config.weapon_secondary_alpha")
                .defineInRange("secondaryAlpha", 0.8, 0.1, 1.0);

        BUILDER.pop();

        BUILDER.comment("Same flat-colour panel style as the player widget (no fade/gradient, no",
                "border/corner decoration) - applies to EVERY element on the widget (icon(s), the",
                "mag number, the reserve number, the fire-mode row) EXCEPT the weapon name toast,",
                "which stays plain floating text. Each element gets its OWN separate box sized to",
                "just that element, with a visible gap between boxes - not one big panel behind",
                "everything, so the widget reads as a cluster of small distinct chips/tiles.")
                .push("panel");

        SHOW_PANEL = BUILDER
                .comment("Draw the individual background chip behind each element (icon(s), mag+",
                        "reserve number, fire-mode row) - everything except the name toast.")
                .translation("modernhud.config.weapon_show_panel")
                .define("enabled", true);

        PANEL_COLOR = BUILDER
                .comment("Panel background colour, as a hex code (e.g. \"0A0D11\" or \"#0A0D11\").")
                .translation("modernhud.config.weapon_panel_color")
                .define("panelColor", ColorConfig.toHex(0x0A0D11));

        PANEL_OPACITY = BUILDER
                .comment("Panel opacity, 0 (fully transparent) to 100 (fully solid/opaque). Kept lower",
                        "than the player widget's by default so it reads as a lighter touch behind the",
                        "icon(s). The secondary weapon slot's panel is additionally scaled by",
                        "secondaryAlpha above, so it recedes along with the dimmer secondary icon.")
                .translation("modernhud.config.weapon_panel_opacity")
                .defineInRange("panelOpacity", 30, 0, 100);

        BORDER_STYLE = BUILDER
                .comment("Edge accent layered on top of the fill above (only draws when it's also on).",
                        "NONE = fill only, no accent. CORNERS = a small tech-HUD corner mark on each",
                        "chip: two short lines per corner (an open \"L\"), no full border - modelled on",
                        "Modern Warfare's own HUD. FULL = a complete thin outline around the whole chip.")
                .translation("modernhud.config.weapon_border_style")
                .defineEnum("borderStyle", PanelBorderStyle.CORNERS);

        BORDER_COLOR = BUILDER
                .comment("Border/corner-bracket colour, as a hex code (e.g. \"E8ECF0\" or \"#E8ECF0\").")
                .translation("modernhud.config.weapon_border_color")
                .define("borderColor", ColorConfig.toHex(0xE8ECF0));

        BORDER_OPACITY = BUILDER
                .comment("Border/corner-bracket opacity, 0-100. Kept brighter/more opaque than the fill",
                        "by default so the accent reads clearly even over a very faint panel.")
                .translation("modernhud.config.weapon_border_opacity")
                .defineInRange("borderOpacity", 90, 0, 100);

        BORDER_LENGTH = BUILDER
                .comment("Length in pixels of each CORNERS-style bracket's two arms (ignored for NONE/",
                        "FULL). This is a maximum, not a fixed size - it's also capped to a fraction of",
                        "each chip's own size, so the same setting looks proportionate on both the tiny",
                        "mag/reserve/fire-mode chips and the bigger icon box, rather than chunky on the",
                        "small ones. Kept short by default - a small accent mark, not a partial border.")
                .translation("modernhud.config.weapon_border_length")
                .defineInRange("borderLength", 3, 1, 20);

        BORDER_OUTSET = BUILDER
                .comment("How far (px) CORNERS-style brackets sit OUTSIDE each chip's own edge, rather",
                        "than flush with it (0 = flush, the old behaviour) - matches the reference HUD,",
                        "where the corner brackets visibly float outside what they're framing rather",
                        "than tracing its border. Kept small by default - too much reads as the",
                        "brackets simply not lining up with what they're supposed to be framing.",
                        "Ignored for FULL/NONE.")
                .translation("modernhud.config.weapon_border_outset")
                .defineInRange("borderOutset", 1, 0, 20);

        SHOW_WEAPON_GLOW = BUILDER
                .comment("A glow accent: a brief bright flash behind the primary icon on weapon",
                        "switch, and a slow breathing pulse around the ammo row while the mag is",
                        "at/below the low-ammo threshold above. Off by default - the current glow",
                        "technique (several offset flat rects with additive blend, no real blur) reads",
                        "as blocky/harsh rather than a soft glow; the mag number's own colour change",
                        "still happens regardless of this setting, so low ammo is never silent either way.",
                        "NOTE: this used to share its config key with the panel/text-panel toggles below",
                        "(all three were accidentally defined as 'panel.enabled'), which silently",
                        "aliased all three together - toggling one toggled the others. Now its own key",
                        "('glowEnabled'); if your old config had this on, it resets to this option's",
                        "own default once.")
                .translation("modernhud.config.weapon_show_glow")
                .define("glowEnabled", false);

        WEAPON_GLOW_COLOR = BUILDER
                .comment("Weapon-switch glow flash colour, as a hex code (e.g. \"E8ECF0\" or \"#E8ECF0\").",
                        "The low-ammo pulse always uses ammoLowColor above instead, so the two glows",
                        "match the colour of what they're each warning about/highlighting.")
                .translation("modernhud.config.weapon_glow_color")
                .define("weaponGlowColor", ColorConfig.toHex(0xE8ECF0));

        SHOW_TEXT_PANEL = BUILDER
                .comment("Separate on/off for the ammo row's own flat background fill, independent of",
                        "showPanel above (which only covers the icon box(es)/fire-mode box now). Back on",
                        "by default: the earlier off-by-default call was working around a GLOW effect",
                        "that didn't line up with its own border - once that was reverted to a plain",
                        "flat fill (same simple fill-then-border technique validated against a separate,",
                        "known-good reference mod's own shadowbox code), there's no more reason for the",
                        "ammo row to be the one element left without a background others still have.",
                        "NOTE: this used to share its config key with showPanel/showWeaponGlow above",
                        "(all three were accidentally defined as 'panel.enabled') - now its own key",
                        "('textPanelEnabled').")
                .translation("modernhud.config.weapon_show_text_panel")
                .define("textPanelEnabled", true);

        TEXT_PANEL_COLOR = BUILDER
                .comment("Ammo/name/fire-mode chip background colour, as a hex code. Defaults to a",
                        "light off-white/grey (unlike the icon box's dark default above) to match",
                        "Modern Warfare's own light, subtle backdrop behind its ammo counter.")
                .translation("modernhud.config.weapon_text_panel_color")
                .define("textPanelColor", ColorConfig.toHex(0xE8ECF0));

        TEXT_PANEL_OPACITY = BUILDER
                .comment("Ammo/name/fire-mode chip opacity, 0-100. Kept low by default so a light",
                        "colour still reads as a subtle tint behind the text, not a solid white block.")
                .translation("modernhud.config.weapon_text_panel_opacity")
                .defineInRange("textPanelOpacity", 14, 0, 100);

        SHOW_AMMO_TYPE_ICON = BUILDER
                .comment("A small \"ammo type\" icon next to the reserve number, reusing the full-auto",
                        "fire-mode icon texture rather than a separate asset. Tinted the same as the",
                        "reserve number (reserveColor/reserveOpacity above) since it sits right next to it.")
                .translation("modernhud.config.weapon_show_ammo_type_icon")
                .define("showAmmoTypeIcon", true);

        AMMO_TYPE_ICON_SCALE = BUILDER
                .comment("Extra scale multiplier applied to the ammo-type icon on top of it already",
                        "tracking the reserve number's own rendered height - the icon reads oversized",
                        "next to the (small, dim) reserve digits at 1.0, so this is kept well under 1",
                        "by default to actually shrink it down to a balanced size (e.g. 0.65 -> icon",
                        "drawn at 65% of the reserve digits' height).")
                .translation("modernhud.config.weapon_ammo_type_icon_scale")
                .defineInRange("ammoTypeIconScale", 0.65, 0.1, 1.5);

        SHOW_ATTACHMENT_DOTS = BUILDER
                .comment("A row of small squares just above the primary icon box, one per attachment",
                        "slot this specific gun actually has (scope/muzzle/stock/grip/laser/extended",
                        "mag - however many of those it allows), filled gold for an equipped slot and",
                        "dim for an empty one - matching the reference HUD's attachment-count dots.")
                .translation("modernhud.config.weapon_show_attachment_dots")
                .define("showAttachmentDots", true);

        ATTACHMENT_DOT_COLOR = BUILDER
                .comment("Attachment dot colour, as a hex code. Only EQUIPPED slots draw a dot at all",
                        "- an empty or unsupported slot draws nothing, not a dim placeholder.")
                .translation("modernhud.config.weapon_attachment_dot_color")
                .define("attachmentDotColor", ColorConfig.toHex(0xFFC94A));

        SECONDARY_DOT_SIZE = BUILDER
                .comment("Size in px of the secondary/neutral weapon box's own corner accent: two",
                        "small filled squares at its BOTTOM corners only (no marks at the top corners",
                        "at all) - a more minimal treatment than the primary icon's full 4-corner",
                        "brackets, matching the reference HUD.")
                .translation("modernhud.config.weapon_secondary_dot_size")
                .defineInRange("secondaryDotSize", 3, 1, 12);

        SHOW_SWITCH_SLIDE = BUILDER
                .comment("The icon box's own \"push up into place\" slide on weapon switch. Independent",
                        "of showSwitchBorderFlash below - run either, both, or neither.")
                .translation("modernhud.config.weapon_show_switch_slide")
                .define("showSwitchSlide", true);

        SWITCH_SLIDE_DISTANCE = BUILDER
                .comment("How far (px, before scale) the icon starts below its resting position on a",
                        "switch. Reduced from this mod's old default - a smaller distance reads as a",
                        "tighter, more deliberate settle rather than a loose bounce.")
                .translation("modernhud.config.weapon_switch_slide_distance")
                .defineInRange("switchSlideDistance", 8, 0, 100);

        SHOW_SWITCH_BORDER_FLASH = BUILDER
                .comment("A brief gold border flash around the primary icon box on weapon switch -",
                        "similar to vanilla Minecraft's own gold-flash accents (e.g. a totem of",
                        "undying's screen flash) - fading from switchBorderFlashColor to the icon's",
                        "normal border colour. Independent of showSwitchSlide above.")
                .translation("modernhud.config.weapon_show_switch_border_flash")
                .define("showSwitchBorderFlash", true);

        SWITCH_BORDER_FLASH_COLOR = BUILDER
                .comment("Switch border-flash colour, as a hex code. Defaults to a warm gold.")
                .translation("modernhud.config.weapon_switch_border_flash_color")
                .define("switchBorderFlashColor", ColorConfig.toHex(0xFFC94A));

        SWITCH_BORDER_FLASH_MS = BUILDER
                .comment("How long (ms) the border-flash fade from switchBorderFlashColor back to the",
                        "normal border colour takes.")
                .translation("modernhud.config.weapon_switch_border_flash_ms")
                .defineInRange("switchBorderFlashMs", 450, 50, 5000);

        BUILDER.pop();

        BUILDER.comment("Text colours - all as hex codes, e.g. \"FFFFFF\" or \"#FFFFFF\" (either works).")
                .push("colors");

        AMMO_COLOR = BUILDER
                .comment("Mag ammo number colour above the low-ammo threshold.")
                .translation("modernhud.config.weapon_ammo_color")
                .define("ammoColor", ColorConfig.toHex(0xFFFFFF));

        AMMO_LOW_COLOR = BUILDER
                .comment("Mag ammo number colour at/below the low-ammo threshold.")
                .translation("modernhud.config.weapon_ammo_low_color")
                .define("ammoLowColor", ColorConfig.toHex(0xE85138));

        LOW_AMMO_THRESHOLD_PERCENT = BUILDER
                .comment("Below this % of mag capacity: the mag number switches to the low-ammo colour,",
                        "AND (if ammo alerts are on below) the pulsing \"LOW AMMO\" alert shows.")
                .translation("modernhud.config.weapon_low_ammo_threshold")
                .defineInRange("lowAmmoThresholdPercent", 20.0, 0.0, 100.0);

        RESERVE_COLOR = BUILDER
                .comment("Reserve ammo number colour (before reserveOpacity is applied below). Kept",
                        "close to white by default - almost the same brightness as the mag number,",
                        "distinguished mainly by its smaller scale (reserveScaleOffset below) rather",
                        "than a dim/greyed-out colour, matching the reference HUD where the reserve",
                        "digits read as slightly smaller/lighter, not a different colour entirely.")
                .translation("modernhud.config.weapon_reserve_color")
                .define("reserveColor", ColorConfig.toHex(0xF2F6FA));

        RESERVE_SCALE_OFFSET = BUILDER
                .comment("How much SMALLER the reserve number's text scale is than the mag number's",
                        "(ammoTextScale above) - e.g. 0.1 means reserve renders at ammoTextScale - 0.1.",
                        "Keeps the reserve number visually secondary to the mag number, matching the",
                        "reference HUD where the reserve digits read as slightly smaller/lighter.")
                .translation("modernhud.config.weapon_reserve_scale_offset")
                .defineInRange("reserveScaleOffset", 0.1, 0.0, 1.0);

        RESERVE_OPACITY = BUILDER
                .comment("Reserve number opacity, 0-100 - kept a bit below full so it reads as visually",
                        "secondary to the (fully opaque) mag number without needing a washed-out colour.")
                .translation("modernhud.config.weapon_reserve_opacity")
                .defineInRange("reserveOpacity", 80, 0, 100);

        NAME_COLOR = BUILDER
                .comment("Weapon name text colour.")
                .translation("modernhud.config.weapon_name_color")
                .define("nameColor", ColorConfig.toHex(0xE6EEF5));

        FIRE_MODE_COLOR = BUILDER
                .comment("Fire mode text colour.")
                .translation("modernhud.config.weapon_fire_mode_color")
                .define("fireModeColor", ColorConfig.toHex(0x9FD3F0));

        BUILDER.pop();

        BUILDER.comment("The \"FIRE TYPE: FULL AUTO\" announcement that flashes centre-screen, above",
                "the crosshair, whenever you switch to a gun (like Modern Warfare's own weapon-pull",
                "announcement) - yellow, then white (held), then fades out completely. The mode name",
                "itself is underlined; the \"FIRE TYPE: \" prefix isn't.")
                .push("fireTypeFlash");

        SHOW_FIRE_TYPE_FLASH = BUILDER
                .comment("Show the \"FIRE TYPE: ...\" flash announcement on weapon switch.")
                .translation("modernhud.config.weapon_show_fire_type_flash")
                .define("enabled", true);

        FIRE_TYPE_FLASH_COLOR = BUILDER
                .comment("Colour during the initial flash phase, hex code.")
                .translation("modernhud.config.weapon_fire_type_flash_color")
                .define("flashColor", ColorConfig.toHex(0xFFD866));

        FIRE_TYPE_HOLD_COLOR = BUILDER
                .comment("Colour during the held phase (after the flash, before it fades out), hex code.")
                .translation("modernhud.config.weapon_fire_type_hold_color")
                .define("holdColor", ColorConfig.toHex(0xFFFFFF));

        FIRE_TYPE_FLASH_MS = BUILDER
                .comment("How long (ms) the initial flash colour lasts before switching to the held colour.")
                .translation("modernhud.config.weapon_fire_type_flash_ms")
                .defineInRange("flashMs", 150, 0, 5000);

        FIRE_TYPE_HOLD_MS = BUILDER
                .comment("How long (ms) the held colour stays fully visible before it starts fading out.")
                .translation("modernhud.config.weapon_fire_type_hold_ms")
                .defineInRange("holdMs", 900, 0, 20000);

        FIRE_TYPE_FADE_MS = BUILDER
                .comment("How long (ms) the fade-out takes once the hold period ends.")
                .translation("modernhud.config.weapon_fire_type_fade_ms")
                .defineInRange("fadeMs", 400, 0, 20000);

        FIRE_TYPE_SCALE = BUILDER
                .comment("Text scale of the announcement - matches ammoAlertScale's default (1.0) so",
                        "the two read as the same size/weight of centre-screen text.")
                .translation("modernhud.config.weapon_fire_type_scale")
                .defineInRange("scale", 1.0, 0.25, 4.0);

        FIRE_TYPE_X_OFFSET = BUILDER
                .comment("Horizontal offset in px from screen centre (positive = further right).")
                .translation("modernhud.config.weapon_fire_type_x_offset")
                .defineInRange("xOffset", 0, -2000, 2000);

        FIRE_TYPE_Y_OFFSET = BUILDER
                .comment("Vertical offset in px from screen centre (positive = further down) - same",
                        "sign convention as ammoAlertYOffset above, so the two can be lined up. Negative",
                        "is allowed on purpose: this used to only be able to move UP from centre, which",
                        "meant it couldn't be tucked down near the ammo readout/alert area at all.")
                .translation("modernhud.config.weapon_fire_type_y_offset")
                .defineInRange("yOffset", 70, -2000, 2000);

        FIRE_TYPE_SHOW_LABEL = BUILDER
                .comment("true = \"FIRE TYPE: FULL AUTO\". false = just \"FULL AUTO\", no label prefix.")
                .translation("modernhud.config.weapon_fire_type_show_label")
                .define("showLabel", true);

        BUILDER.pop();

        BUILDER.comment("Pulsing centre-screen text for RELOAD / NO AMMO / LOW AMMO - matches",
                "TaczPresence's own alert convention so the two feel consistent if you've used both.",
                "Skipped entirely for guns that read ammo straight from the inventory (there's no",
                "separate mag to run dry) and while overheat-locked.")
                .push("ammoAlerts");

        SHOW_AMMO_ALERTS = BUILDER
                .comment("Show the RELOAD / NO AMMO / LOW AMMO pulsing alert text.")
                .translation("modernhud.config.weapon_show_ammo_alerts")
                .define("enabled", true);

        RELOAD_ALERT_COLOR = BUILDER
                .comment("\"RELOAD\" alert colour (mag empty, reserve available), hex code.")
                .translation("modernhud.config.weapon_reload_alert_color")
                .define("reloadAlertColor", ColorConfig.toHex(0xFFD866));

        NO_AMMO_ALERT_COLOR = BUILDER
                .comment("\"NO AMMO\" alert colour (mag AND reserve both empty), hex code.")
                .translation("modernhud.config.weapon_no_ammo_alert_color")
                .define("noAmmoAlertColor", ColorConfig.toHex(0xE85138));

        LOW_AMMO_ALERT_COLOR = BUILDER
                .comment("\"LOW AMMO\" alert colour (mag below the low-ammo threshold above), hex code.")
                .translation("modernhud.config.weapon_low_ammo_alert_color")
                .define("lowAmmoAlertColor", ColorConfig.toHex(0xFFD866));

        AMMO_ALERT_SCALE = BUILDER
                .comment("Scale of the alert text.")
                .translation("modernhud.config.weapon_ammo_alert_scale")
                .defineInRange("ammoAlertScale", 1.0, 0.25, 4.0);

        AMMO_ALERT_Y_OFFSET = BUILDER
                .comment("Vertical offset in px from screen centre (positive = further down).")
                .translation("modernhud.config.weapon_ammo_alert_y_offset")
                .defineInRange("ammoAlertYOffset", 55, -2000, 2000);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        preferredHudSource = PREFERRED_HUD_SOURCE.get();
        showName = SHOW_NAME.get();
        showFireMode = SHOW_FIRE_MODE.get();
        showReserveAmmo = SHOW_RESERVE_AMMO.get();

        position = POSITION.get();
        offsetX = OFFSET_X.get();
        offsetY = OFFSET_Y.get();
        scale = SCALE.get();

        iconScale = ICON_SCALE.get();
        ammoTextScale = AMMO_TEXT_SCALE.get();
        frameWidth = FRAME_WIDTH.get();
        frameHeight = FRAME_HEIGHT.get();
        zeroPadAmmo = ZERO_PAD_AMMO.get();

        showSecondary = SHOW_SECONDARY.get();
        secondaryIconScale = SECONDARY_ICON_SCALE.get();
        secondaryAlpha = SECONDARY_ALPHA.get();


        showPanel = SHOW_PANEL.get();
        panelColor = ColorConfig.fromHex(PANEL_COLOR.get(), 0x0A0D11);
        panelOpacity = PANEL_OPACITY.get();
        borderStyle = BORDER_STYLE.get();
        borderColor = ColorConfig.fromHex(BORDER_COLOR.get(), 0xE8ECF0);
        borderOpacity = BORDER_OPACITY.get();
        borderLength = BORDER_LENGTH.get();
        borderOutset = BORDER_OUTSET.get();
        showWeaponGlow = SHOW_WEAPON_GLOW.get();
        weaponGlowColor = ColorConfig.fromHex(WEAPON_GLOW_COLOR.get(), 0xE8ECF0);
        showTextPanel = SHOW_TEXT_PANEL.get();
        textPanelColor = ColorConfig.fromHex(TEXT_PANEL_COLOR.get(), 0xE8ECF0);
        textPanelOpacity = TEXT_PANEL_OPACITY.get();
        showAmmoTypeIcon = SHOW_AMMO_TYPE_ICON.get();
        ammoTypeIconScale = AMMO_TYPE_ICON_SCALE.get();
        showAttachmentDots = SHOW_ATTACHMENT_DOTS.get();
        attachmentDotColor = ColorConfig.fromHex(ATTACHMENT_DOT_COLOR.get(), 0xFFC94A);
        secondaryDotSize = SECONDARY_DOT_SIZE.get();
        showSwitchSlide = SHOW_SWITCH_SLIDE.get();
        switchSlideDistance = SWITCH_SLIDE_DISTANCE.get();
        showSwitchBorderFlash = SHOW_SWITCH_BORDER_FLASH.get();
        switchBorderFlashColor = ColorConfig.fromHex(SWITCH_BORDER_FLASH_COLOR.get(), 0xFFC94A);
        switchBorderFlashMs = SWITCH_BORDER_FLASH_MS.get();

        ammoColor = ColorConfig.fromHex(AMMO_COLOR.get(), 0xFFFFFF);
        ammoLowColor = ColorConfig.fromHex(AMMO_LOW_COLOR.get(), 0xE85138);
        lowAmmoThresholdPercent = LOW_AMMO_THRESHOLD_PERCENT.get();
        reserveColor = ColorConfig.fromHex(RESERVE_COLOR.get(), 0xF2F6FA);
        reserveScaleOffset = RESERVE_SCALE_OFFSET.get();
        reserveOpacity = RESERVE_OPACITY.get();
        nameColor = ColorConfig.fromHex(NAME_COLOR.get(), 0xE6EEF5);
        fireModeColor = ColorConfig.fromHex(FIRE_MODE_COLOR.get(), 0x9FD3F0);

        showFireTypeFlash = SHOW_FIRE_TYPE_FLASH.get();
        fireTypeFlashColor = ColorConfig.fromHex(FIRE_TYPE_FLASH_COLOR.get(), 0xFFD866);
        fireTypeHoldColor = ColorConfig.fromHex(FIRE_TYPE_HOLD_COLOR.get(), 0xFFFFFF);
        fireTypeFlashMs = FIRE_TYPE_FLASH_MS.get();
        fireTypeHoldMs = FIRE_TYPE_HOLD_MS.get();
        fireTypeFadeMs = FIRE_TYPE_FADE_MS.get();
        fireTypeScale = FIRE_TYPE_SCALE.get();
        fireTypeXOffset = FIRE_TYPE_X_OFFSET.get();
        fireTypeYOffset = FIRE_TYPE_Y_OFFSET.get();
        fireTypeShowLabel = FIRE_TYPE_SHOW_LABEL.get();

        showAmmoAlerts = SHOW_AMMO_ALERTS.get();
        reloadAlertColor = ColorConfig.fromHex(RELOAD_ALERT_COLOR.get(), 0xFFD866);
        noAmmoAlertColor = ColorConfig.fromHex(NO_AMMO_ALERT_COLOR.get(), 0xE85138);
        lowAmmoAlertColor = ColorConfig.fromHex(LOW_AMMO_ALERT_COLOR.get(), 0xFFD866);
        ammoAlertScale = AMMO_ALERT_SCALE.get();
        ammoAlertYOffset = AMMO_ALERT_Y_OFFSET.get();

        // Sync every side of the 3-way HUD selector every time this config (re)loads - including
        // from the in-game config screen, not just at startup. See TaczCompat's javadoc for why
        // this exists (multiple HUDs rendering at once is what caused the "icon lệch" bug).
        syncHudSource();
    }

    /** The actual 3-way switch: makes exactly one weapon HUD source active, everywhere. Public so
     *  {@code ClientEvents} can also re-assert it once at client setup, in case TaczPresence's own
     *  config finishes loading after ours (load-order safety net; harmless either way). */
    public static void syncHudSource() {
        enabled = preferredHudSource == WeaponHudSource.MODERNHUD;
        cc.xypp.modernhud.client.compat.TaczCompat.setVanillaGunHudEnabled(!enabled);
        if (preferredHudSource != WeaponHudSource.MODERNHUD) {
            cc.xypp.modernhud.client.compat.TaczCompat.setPresenceHudType(
                    preferredHudSource == WeaponHudSource.TACZ_P_HUD);
        }
    }

    // No runtime position setter here on purpose: unlike the player widget and the feedback icon,
    // the weapon widget's position is config-file-only (modernhud-weapon.toml), not draggable in
    // the in-game HUD editor - see the editor's class javadoc for why.

    /**
     * Advances the 3-way HUD selector for the toggle keybind: MODERNHUD -&gt; TACZ_HUD -&gt;
     * TACZ_P_HUD -&gt; back to MODERNHUD, persisting to disk. Skips TACZ_P_HUD in the cycle when
     * TaczPresence isn't actually installed, since picking it would be indistinguishable from
     * TACZ_HUD anyway.
     */
    public static void cycleHudSource() {
        WeaponHudSource next = switch (preferredHudSource) {
            case MODERNHUD -> WeaponHudSource.TACZ_HUD;
            case TACZ_HUD -> cc.xypp.modernhud.client.compat.TaczCompat.presenceLoaded()
                    ? WeaponHudSource.TACZ_P_HUD : WeaponHudSource.MODERNHUD;
            case TACZ_P_HUD -> WeaponHudSource.MODERNHUD;
        };
        preferredHudSource = next;
        PREFERRED_HUD_SOURCE.set(next);
        syncHudSource();
    }
}
