package cc.xypp.modernhud.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client-only configuration for Modern HUD's personal widget and hit/break
 * feedback icon - what they look like and where they sit on screen. Whether
 * the feedback events fire at all (and who they're sent to) is decided
 * server-side, in CommonConfig.
 */
@Mod.EventBusSubscriber(modid = "modernhud", bus = Mod.EventBusSubscriber.Bus.MOD)
public class HudConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- General widget ---
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue SHOW_HEALTH_BAR;
    private static final ForgeConfigSpec.BooleanValue SHOW_NAME;
    private static final ForgeConfigSpec.BooleanValue SHOW_HEAD;

    // --- Team HUD: vanilla-scoreboard-team teammate health list, stacked above the widget above ---
    private static final ForgeConfigSpec.BooleanValue SHOW_TEAM_HUD;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_MAX_PLAYERS;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_BAR_WIDTH;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_ROW_GAP;
    private static final ForgeConfigSpec.IntValue TEAM_HUD_GROUP_GAP;
    private static final ForgeConfigSpec.BooleanValue TEAM_HUD_SHOW_HEAD;
    private static final ForgeConfigSpec.BooleanValue SHOW_TEAM_ABSORPTION;
    private static final ForgeConfigSpec.BooleanValue TEAM_HUD_USE_TEAM_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> TEAM_HUD_NAME_COLOR;

    // --- HP number display ---
    private static final ForgeConfigSpec.BooleanValue SHOW_HP_TEXT;
    private static final ForgeConfigSpec.EnumValue<HpTextFormat> HP_TEXT_FORMAT;
    private static final ForgeConfigSpec.EnumValue<BarTextPosition> HP_TEXT_POSITION;
    private static final ForgeConfigSpec.DoubleValue LOW_HEALTH_THRESHOLD_PERCENT;
    private static final ForgeConfigSpec.BooleanValue SHOW_VITALS_GLOW;
    private static final ForgeConfigSpec.ConfigValue<String> VITALS_GLOW_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> HP_TEXT_NORMAL_COLOR;
    private static final ForgeConfigSpec.ConfigValue<String> HP_TEXT_LOW_COLOR;

    // --- Armor readout (vanilla armor points, 0-20) — TEXT ONLY, no bar ---
    private static final ForgeConfigSpec.BooleanValue SHOW_ARMOR;
    private static final ForgeConfigSpec.EnumValue<HpTextFormat> ARMOR_TEXT_FORMAT;
    private static final ForgeConfigSpec.EnumValue<BarTextPosition> ARMOR_TEXT_POSITION;
    private static final ForgeConfigSpec.ConfigValue<String> ARMOR_TEXT_COLOR;

    // --- Widget position ---
    private static final ForgeConfigSpec.EnumValue<HudPosition> POSITION;
    private static final ForgeConfigSpec.IntValue OFFSET_X;
    private static final ForgeConfigSpec.IntValue OFFSET_Y;

    // --- Bar appearance ---
    private static final ForgeConfigSpec.IntValue BAR_WIDTH;
    private static final ForgeConfigSpec.IntValue BAR_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue BAR_SCALE;
    private static final ForgeConfigSpec.IntValue SEGMENT_COUNT;
    private static final ForgeConfigSpec.DoubleValue POINTS_PER_SEGMENT;
    private static final ForgeConfigSpec.BooleanValue SHOW_OVERFLOW_TEXT;
    private static final ForgeConfigSpec.EnumValue<NamePosition> NAME_POSITION;
    private static final ForgeConfigSpec.IntValue ABSORPTION_HEIGHT;
    private static final ForgeConfigSpec.EnumValue<AbsorptionColorMode> ABSORPTION_COLOR_MODE;
    private static final ForgeConfigSpec.ConfigValue<String> ABSORPTION_CUSTOM_COLOR;

    // --- Background panel (flat colour, adjustable opacity, behind the widget) ---
    private static final ForgeConfigSpec.BooleanValue SHOW_PANEL;
    private static final ForgeConfigSpec.ConfigValue<String> PANEL_COLOR;
    private static final ForgeConfigSpec.IntValue PANEL_OPACITY;
    private static final ForgeConfigSpec.EnumValue<PanelBorderStyle> BORDER_STYLE;
    private static final ForgeConfigSpec.ConfigValue<String> BORDER_COLOR;
    private static final ForgeConfigSpec.IntValue BORDER_OPACITY;
    private static final ForgeConfigSpec.IntValue BORDER_LENGTH;
    private static final ForgeConfigSpec.IntValue BORDER_OUTSET;

    // --- Hit / break feedback sounds (played locally on this client only) ---
    private static final ForgeConfigSpec.DoubleValue HIT_SOUND_VOLUME;
    private static final ForgeConfigSpec.DoubleValue BREAK_SOUND_VOLUME;
    private static final ForgeConfigSpec.BooleanValue SHOW_FEEDBACK_WHEN_VICTIM;

    // --- Hit / break feedback icon: position (same anchor system as the main widget) ---
    private static final ForgeConfigSpec.EnumValue<HudPosition> FEEDBACK_POSITION;
    private static final ForgeConfigSpec.IntValue FEEDBACK_OFFSET_X;
    private static final ForgeConfigSpec.IntValue FEEDBACK_OFFSET_Y;
    private static final ForgeConfigSpec.IntValue FEEDBACK_ICON_WIDTH;
    private static final ForgeConfigSpec.IntValue FEEDBACK_ICON_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue FEEDBACK_ICON_SCALE;

    // --- Hit / break feedback icon: which ones show + how long ---
    private static final ForgeConfigSpec.BooleanValue SHOW_HIT_ICON;
    private static final ForgeConfigSpec.IntValue HIT_ICON_DURATION_MS;
    private static final ForgeConfigSpec.BooleanValue SHOW_BREAK_ICON;
    private static final ForgeConfigSpec.IntValue BREAK_ICON_DURATION_MS;

    public static final ForgeConfigSpec SPEC;

    // Plain fields used by the renderers
    public static boolean enabled;
    public static boolean showHealthBar;
    public static boolean showName;
    public static boolean showHead;

    public static boolean showTeamHud;
    public static int teamHudMaxPlayers;
    public static int teamHudBarWidth;
    public static int teamHudRowGap;
    public static int teamHudGroupGap;
    public static boolean teamHudShowHead;
    public static boolean showTeamAbsorption;
    public static boolean teamHudUseTeamColor;
    public static int teamHudNameColor;

    public static boolean showHpText;
    public static HpTextFormat hpTextFormat;
    public static BarTextPosition hpTextPosition;
    public static double lowHealthThresholdPercent;
    public static boolean showVitalsGlow;
    public static int vitalsGlowColor;
    public static int hpTextNormalColor;
    public static int hpTextLowColor;

    public static boolean showArmor;
    public static HpTextFormat armorTextFormat;
    public static BarTextPosition armorTextPosition;
    public static int armorTextColor;
    public static HudPosition position;
    public static int offsetX;
    public static int offsetY;
    public static int barWidth;
    public static int barHeight;
    public static double barScale;
    public static int segmentCount;
    public static double pointsPerSegment;
    public static boolean showOverflowText;
    public static NamePosition namePosition;
    public static int absorptionHeight;
    public static AbsorptionColorMode absorptionColorMode;
    public static int absorptionCustomColor;

    public static boolean showPanel;
    public static int panelColor;
    public static double panelOpacity;
    public static PanelBorderStyle borderStyle;
    public static int borderColor;
    public static double borderOpacity;
    public static int borderLength;
    public static int borderOutset;

    public static float hitSoundVolume;
    public static float breakSoundVolume;
    public static boolean showFeedbackWhenVictim;

    public static HudPosition feedbackPosition;
    public static int feedbackOffsetX;
    public static int feedbackOffsetY;
    public static int feedbackIconWidth;
    public static int feedbackIconHeight;
    public static double feedbackIconScale;

    public static boolean showHitIcon;
    public static int hitIconDurationMs;
    public static boolean showBreakIcon;
    public static int breakIconDurationMs;

    static {
        BUILDER.comment("Absorption HUD - client-only display settings").push("general");

        ENABLED = BUILDER
                .comment("Master switch for the whole HUD widget (bar + health + name + head).")
                .translation("modernhud.config.enabled")
                .define("enabled", true);

        SHOW_HEALTH_BAR = BUILDER
                .comment("Show your own health bar under the absorption bar.")
                .translation("modernhud.config.show_health_bar")
                .define("showHealthBar", true);

        SHOW_NAME = BUILDER
                .comment("Show your own username next to the head icon.")
                .translation("modernhud.config.show_name")
                .define("showName", false); // Decade: no name

        SHOW_HEAD = BUILDER
                .comment("Show a small 2D render of your own head/skin.")
                .translation("modernhud.config.show_head")
                .define("showHead", true); // Decade: the face is the anchor

        BUILDER.pop();

        BUILDER.comment("Numeric HP display, e.g. '20/20' or '100%'.").push("hpText");

        SHOW_HP_TEXT = BUILDER
                .comment("Show your current HP as text somewhere on the widget.")
                .translation("modernhud.config.show_hp_text")
                .define("enabled", true);

        HP_TEXT_FORMAT = BUILDER
                .comment("CLASSIC = '20.0/20.0' (current/max HP points). WHOLE = '48/60'. PERCENT = '100%'.")
                .translation("modernhud.config.hp_text_format")
                .defineEnum("format", HpTextFormat.WHOLE); // Decade: 60-point health, whole numbers

        HP_TEXT_POSITION = BUILDER
                .comment("Where the HP text sits:",
                        "BELOW_BAR - a new line under the health bar",
                        "BELOW_NAME_HEAD - a new line under the head/name row",
                        "END_OF_BAR - right after the health bar's right edge, same line")
                .translation("modernhud.config.hp_text_position")
                .defineEnum("position", BarTextPosition.END_OF_BAR);

        LOW_HEALTH_THRESHOLD_PERCENT = BUILDER
                .comment("Below this % of max HP, the text switches to the 'low health' colour",
                        "as a quick at-a-glance warning.")
                .translation("modernhud.config.low_health_threshold_percent")
                .defineInRange("lowHealthThresholdPercent", 33.4, 0.0, 100.0); // Decade: a third, as decade_hud's low-health edge

        SHOW_VITALS_GLOW = BUILDER
                .comment("A glow behind the health bar (and the absorption bar, on a shield-breaking",
                        "hit): a brief bright flash when you take damage, and a slow breathing pulse",
                        "while health is at/below the low-health threshold above. Off by default - the",
                        "current glow technique (several offset flat rects with additive blend, no real",
                        "blur) reads as blocky/harsh rather than a soft glow; the bar's own colour",
                        "change and the HP text colour change still happen regardless of this setting,",
                        "so low health is never silent either way.",
                        "NOTE: this used to share its config key with 'showHpText' above (both were",
                        "accidentally defined as 'hpText.enabled'), which silently aliased the two -",
                        "toggling one also toggled the other. Now its own key ('glowEnabled'); if your",
                        "old config had this on, it will reset to this option's own default once.")
                .translation("modernhud.config.show_vitals_glow")
                .define("glowEnabled", true); // Decade: the flash and the low-health breathing are wanted

        VITALS_GLOW_COLOR = BUILDER
                .comment("Vitals glow colour, as a hex code (e.g. \"FF3B30\" or \"#FF3B30\").")
                .translation("modernhud.config.vitals_glow_color")
                .define("vitalsGlowColor", ColorConfig.toHex(0xFF3B30));

        HP_TEXT_NORMAL_COLOR = BUILDER
                .comment("HP text colour above the low-health threshold, as a hex code (e.g. \"FFFFFF\").")
                .translation("modernhud.config.hp_text_normal_color")
                .define("normalColor", ColorConfig.toHex(0xFFFFFF));

        HP_TEXT_LOW_COLOR = BUILDER
                .comment("HP text colour at/below the low-health threshold, as a hex code.")
                .translation("modernhud.config.hp_text_low_color")
                .define("lowColor", ColorConfig.toHex(0xFF5555));

        BUILDER.pop();

        BUILDER.comment("Armor readout - vanilla armor points (0-20), shown as TEXT ONLY (e.g.",
                "'14/20'), no bar. Shown whenever enabled, even at 0, since armor is persistent",
                "gear info (like vanilla's own armor icons).")
                .push("armor");

        SHOW_ARMOR = BUILDER
                .comment("Show your current armor points as text.")
                .translation("modernhud.config.show_armor")
                .define("enabled", false);

        ARMOR_TEXT_FORMAT = BUILDER
                .comment("CLASSIC = '12/20' (current/max armor points). PERCENT = '60%'.")
                .translation("modernhud.config.armor_text_format")
                .defineEnum("textFormat", HpTextFormat.CLASSIC);

        ARMOR_TEXT_POSITION = BUILDER
                .comment("BELOW_NAME_HEAD = shares the info line above the bars, right after the HP",
                        "text. END_OF_BAR / BELOW_BAR = its own row directly under the health bar",
                        "(there's no armor bar to be relative to any more, so both just mean 'own row').")
                .translation("modernhud.config.armor_text_position")
                .defineEnum("textPosition", BarTextPosition.END_OF_BAR);

        ARMOR_TEXT_COLOR = BUILDER
                .comment("Armor text colour, as a hex code (e.g. \"C0C0C0\" or \"#C0C0C0\").")
                .translation("modernhud.config.armor_text_color")
                .define("textColor", ColorConfig.toHex(0xC0C0C0));

        BUILDER.pop();

        BUILDER.comment("Where the widget is anchored on screen").push("position");

        POSITION = BUILDER
                .comment("Screen corner the widget is anchored to. Use CUSTOM to position it freely with offsetX/offsetY.",
                        "Allowed values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM")
                .translation("modernhud.config.position")
                .defineEnum("anchor", HudPosition.BOTTOM_LEFT);

        OFFSET_X = BUILDER
                .comment("Horizontal offset in pixels from the chosen anchor (or absolute X when anchor = CUSTOM).")
                .translation("modernhud.config.offset_x")
                .defineInRange("offsetX", 12, -10000, 10000);

        OFFSET_Y = BUILDER
                .comment("Vertical offset in pixels from the chosen anchor (or absolute Y when anchor = CUSTOM).")
                .translation("modernhud.config.offset_y")
                .defineInRange("offsetY", 12, -10000, 10000);

        BUILDER.pop();

        BUILDER.comment("Bar size and how Absorption points map onto it").push("appearance");

        BAR_WIDTH = BUILDER
                .comment("Width of the bars in pixels (before scale is applied).")
                .translation("modernhud.config.bar_width")
                .defineInRange("barWidth", 76, 10, 1000);

        BAR_HEIGHT = BUILDER
                .comment("Height of each bar in pixels (before scale is applied).")
                .translation("modernhud.config.bar_height")
                .defineInRange("barHeight", 6, 2, 200);

        BAR_SCALE = BUILDER
                .comment("Overall scale multiplier applied to the whole widget.")
                .translation("modernhud.config.bar_scale")
                .defineInRange("barScale", 1.0, 0.1, 5.0);

        SEGMENT_COUNT = BUILDER
                .comment("Number of visual segments the absorption bar is split into.",
                        "4 is the intended default: it lines up exactly with the white/blue/purple/red",
                        "colour tiers, and with vanilla's 4 Absorption effect levels (I-IV).")
                .translation("modernhud.config.segment_count")
                .defineInRange("segmentCount", 3, 1, 20);

        POINTS_PER_SEGMENT = BUILDER
                .comment("How many Absorption points fill one segment. Vanilla Absorption gives 4 points per level",
                        "(2 hearts). Default of 4.0 means one segment = one Absorption level.")
                .translation("modernhud.config.points_per_segment")
                .defineInRange("pointsPerSegment", 4.0, 0.5, 100.0);

        SHOW_OVERFLOW_TEXT = BUILDER
                .comment("If an entity's actual Absorption exceeds what the bar can visually show",
                        "(e.g. stacked repeatedly via /effect give, or a modded source above vanilla's",
                        "normal max), the bar alone would look 'full and not going down' even while it's",
                        "genuinely decreasing from a much higher hidden number. With this on, a small",
                        "'+N' label is drawn next to the bar showing exactly how much extra there is.")
                .translation("modernhud.config.show_overflow_text")
                .define("showOverflowText", false);

        NAME_POSITION = BUILDER
                .comment("Where your name shows relative to the bars: ABOVE (MW default) or BELOW.")
                .translation("modernhud.config.name_position")
                .defineEnum("namePosition", NamePosition.ABOVE);

        ABSORPTION_HEIGHT = BUILDER
                .comment("Height in px of the (thinner) absorption strip that sits ABOVE the armor bar,",
                        "MW-style. Defaults to roughly half the main bar height.")
                .translation("modernhud.config.absorption_height")
                .defineInRange("absorptionHeight", 3, 1, 100);

        ABSORPTION_COLOR_MODE = BUILDER
                .comment("TIERED = the plates climb gray -> blue -> purple -> red as more of them fill",
                        "(1 full plate = gray, 2 = blue, 3 = purple, 4+ = red).",
                        "CUSTOM = every plate always uses absorptionCustomColor below, regardless of",
                        "how many are filled - e.g. lock the whole bar to one flat ocean blue (the default).")
                .translation("modernhud.config.absorption_color_mode")
                .defineEnum("absorptionColorMode", AbsorptionColorMode.CUSTOM);

        ABSORPTION_CUSTOM_COLOR = BUILDER
                .comment("Fixed absorption plate colour used when absorptionColorMode = CUSTOM,",
                        "as a hex code (e.g. \"1C7ED6\" or \"#1C7ED6\"). Default is a full ocean blue.")
                .translation("modernhud.config.absorption_custom_color")
                .define("absorptionCustomColor", ColorConfig.toHex(0xD8D8D0)); // Decade: plates are bone white

        BUILDER.pop();

        BUILDER.comment("A flat-colour background panel behind the whole widget - NOT a fade/gradient,",
                "just one solid colour at whatever opacity you set, optionally with small corner",
                "accent marks (see below) - no continuous border either way.")
                .push("panel");

        SHOW_PANEL = BUILDER
                .comment("Draw the background panel behind the whole widget.")
                .translation("modernhud.config.show_panel")
                .define("enabled", false);

        PANEL_COLOR = BUILDER
                .comment("Panel background colour, as a hex code (e.g. \"0A0D11\" or \"#0A0D11\").")
                .translation("modernhud.config.panel_color")
                .define("panelColor", ColorConfig.toHex(0x0A0D11));

        PANEL_OPACITY = BUILDER
                .comment("Panel opacity, 0 (fully transparent - same as turning the panel off) to",
                        "100 (fully solid/opaque).")
                .translation("modernhud.config.panel_opacity")
                .defineInRange("panelOpacity", 45, 0, 100);

        BORDER_STYLE = BUILDER
                .comment("Edge accent layered on top of the fill above (only draws when it's also on).",
                        "NONE = fill only, no accent. CORNERS = a small tech-HUD corner mark: two short",
                        "lines per corner (an open \"L\"), no full border - modelled on Modern Warfare's",
                        "own HUD. FULL = a complete thin outline around the whole panel.")
                .translation("modernhud.config.border_style")
                .defineEnum("borderStyle", PanelBorderStyle.CORNERS);

        BORDER_COLOR = BUILDER
                .comment("Border/corner-bracket colour, as a hex code (e.g. \"E8ECF0\" or \"#E8ECF0\").")
                .translation("modernhud.config.border_color")
                .define("borderColor", ColorConfig.toHex(0xE8ECF0));

        BORDER_OPACITY = BUILDER
                .comment("Border/corner-bracket opacity, 0-100. Kept brighter/more opaque than the fill",
                        "by default so the accent reads clearly even over a very faint panel.")
                .translation("modernhud.config.border_opacity")
                .defineInRange("borderOpacity", 90, 0, 100);

        BORDER_LENGTH = BUILDER
                .comment("Length in pixels of each CORNERS-style bracket's two arms (ignored for NONE/",
                        "FULL). This is a maximum, not a fixed size - it's also capped to a fraction of",
                        "the panel's own size, so it looks proportionate rather than chunky on a small",
                        "panel. Kept short by default - a small accent mark, not a partial border.")
                .translation("modernhud.config.border_length")
                .defineInRange("borderLength", 2, 1, 20);

        BORDER_OUTSET = BUILDER
                .comment("How far (px) CORNERS-style brackets sit OUTSIDE the panel's own edge, rather",
                        "than flush with it (0 = flush, the old behaviour) - matches the reference HUD,",
                        "where the corner brackets visibly float outside what they're framing rather",
                        "than tracing its border. Kept small by default - too much reads as the",
                        "brackets simply not lining up with what they're supposed to be framing.",
                        "Ignored for FULL/NONE.")
                .translation("modernhud.config.border_outset")
                .defineInRange("borderOutset", 2, 0, 20);

        BUILDER.pop();

        BUILDER.comment("Volume of the hit/break sounds. These always play LOCALLY ONLY, to you -",
                "never broadcast to other nearby players - so this is purely your own preference.")
                .push("sounds");

        HIT_SOUND_VOLUME = BUILDER
                .comment("Volume of the 'hit landed on active shield' sound (0.0 - 1.0).")
                .translation("modernhud.config.hit_sound_volume")
                .defineInRange("hitSoundVolume", 0.6, 0.0, 1.0);

        BREAK_SOUND_VOLUME = BUILDER
                .comment("Volume of the 'shield fully broken' sound (0.0 - 1.0).")
                .translation("modernhud.config.break_sound_volume")
                .defineInRange("breakSoundVolume", 0.7, 0.0, 1.0);

        SHOW_FEEDBACK_WHEN_VICTIM = BUILDER
                .comment("Whether YOU get the hit/break sound + icon when YOU are the one getting hit.",
                        "Turn this off if you find it distracting to hear/see it while taking damage.",
                        "This never affects the attacker's side - they always get their feedback.")
                .translation("modernhud.config.show_feedback_when_victim")
                .define("showFeedbackWhenVictim", true);

        BUILDER.pop();

        BUILDER.comment("Hit/break feedback icon position and size.",
                "Uses the exact same anchor system as the main widget above, so you can put it",
                "anywhere on screen - it is NOT locked next to the crosshair.")
                .push("feedbackIcon");

        FEEDBACK_POSITION = BUILDER
                .comment("Screen corner the icon is anchored to, or CUSTOM for a free absolute position.",
                        "Allowed values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM")
                .translation("modernhud.config.feedback_position")
                .defineEnum("anchor", HudPosition.CUSTOM);

        FEEDBACK_OFFSET_X = BUILDER
                .comment("Horizontal offset in pixels from the chosen anchor (or absolute X when anchor = CUSTOM).")
                .translation("modernhud.config.feedback_offset_x")
                .defineInRange("offsetX", 377, -10000, 10000);

        FEEDBACK_OFFSET_Y = BUILDER
                .comment("Vertical offset in pixels from the chosen anchor (or absolute Y when anchor = CUSTOM).")
                .translation("modernhud.config.feedback_offset_y")
                .defineInRange("offsetY", 132, -10000, 10000);

        FEEDBACK_ICON_WIDTH = BUILDER
                .comment("Icon width in pixels (before scale). The source images are square (256x256).")
                .translation("modernhud.config.feedback_icon_width")
                .defineInRange("width", 28, 4, 500);

        FEEDBACK_ICON_HEIGHT = BUILDER
                .comment("Icon height in pixels (before scale).")
                .translation("modernhud.config.feedback_icon_height")
                .defineInRange("height", 28, 4, 500);

        FEEDBACK_ICON_SCALE = BUILDER
                .comment("Extra scale multiplier for the icon.")
                .translation("modernhud.config.feedback_icon_scale")
                .defineInRange("scale", 1.0, 0.1, 10.0);

        BUILDER.pop();

        BUILDER.comment("Which icons show, and for how long (milliseconds) before fading out.")
                .push("feedbackIconTiming");

        SHOW_HIT_ICON = BUILDER
                .comment("Show an icon when a hit lands on an active shield without fully breaking it.")
                .translation("modernhud.config.show_hit_icon")
                .define("showHitIcon", false); // Decade: nothing triggers it (client-only)

        HIT_ICON_DURATION_MS = BUILDER
                .comment("How long the hit icon stays on screen, in milliseconds.")
                .translation("modernhud.config.hit_icon_duration_ms")
                .defineInRange("hitIconDurationMs", 300, 50, 10000);

        SHOW_BREAK_ICON = BUILDER
                .comment("Show an icon when you (the attacker) fully break someone's Absorption.")
                .translation("modernhud.config.show_break_icon")
                .define("showBreakIcon", false); // Decade: nothing triggers it (client-only)

        BREAK_ICON_DURATION_MS = BUILDER
                .comment("How long the break icon stays on screen, in milliseconds.")
                .translation("modernhud.config.break_icon_duration_ms")
                .defineInRange("breakIconDurationMs", 700, 50, 10000);

        BUILDER.pop();

        BUILDER.comment("Teammate health list (vanilla scoreboard team - /team join, same team as you),",
                "Warzone-style: one compact row per teammate, stacked directly above your own widget",
                "above. Uses the SAME server-authoritative team membership vanilla itself uses, no",
                "separate team system of its own. A teammate only shows up here while their entity is",
                "actually loaded/tracked on your client (the same range vanilla shows other players'",
                "names/health-bar overlays at) - someone too far away or on another server-side",
                "dimension simply won't appear until they're back in range, same as any other",
                "client-side player info.")
                .push("teamHud");

        SHOW_TEAM_HUD = BUILDER
                .comment("Show the teammate health list. Nothing renders at all if you aren't on a",
                        "team, or your team has no other tracked members.")
                .translation("modernhud.config.show_team_hud")
                .define("enabled", false); // Decade: teams are not scoreboard teams

        TEAM_HUD_MAX_PLAYERS = BUILDER
                .comment("Maximum teammates shown at once. Extra teammates beyond this are simply",
                        "hidden (sorted alphabetically by name, so it's at least a stable/predictable",
                        "cutoff) - shrink your team, or wait for someone to leave, to bring a hidden",
                        "one back into view.")
                .translation("modernhud.config.team_hud_max_players")
                .defineInRange("maxPlayers", 6, 1, 16);

        TEAM_HUD_BAR_WIDTH = BUILDER
                .comment("Fixed width in px (before scale) of each teammate's health bar. Rows don't",
                        "resize for long names (same reasoning as the main widget's fixed frame) - a",
                        "very long name may run past this width, it just won't push the bar over.")
                .translation("modernhud.config.team_hud_bar_width")
                .defineInRange("barWidth", 76, 16, 400);

        TEAM_HUD_ROW_GAP = BUILDER
                .comment("Vertical gap in px between teammate rows.")
                .translation("modernhud.config.team_hud_row_gap")
                .defineInRange("rowGap", 2, 0, 40);

        TEAM_HUD_GROUP_GAP = BUILDER
                .comment("Vertical gap in px between the team list and your own widget it stacks above.")
                .translation("modernhud.config.team_hud_group_gap")
                .defineInRange("groupGap", 4, 0, 100);

        TEAM_HUD_SHOW_HEAD = BUILDER
                .comment("Show each teammate's head icon next to their row, like your own widget's head.")
                .translation("modernhud.config.team_hud_show_head")
                .define("showHead", false);

        SHOW_TEAM_ABSORPTION = BUILDER
                .comment("Show each teammate's Absorption (shield) bar as its own row above their",
                        "health bar - same segmented-plate look, colour mode (tiered/custom) and",
                        "segment/points settings as your own widget's Absorption bar above, so the two",
                        "read as one consistent style. Glides smoothly to the real value, same as the",
                        "health bar below it. A teammate currently in Spectator mode always shows this",
                        "as empty, even if the server hasn't reset their actual Absorption points yet -",
                        "spectating never leaks a stale/leftover shield reading onto their row.")
                .translation("modernhud.config.team_hud_show_absorption")
                .define("showAbsorption", true);

        TEAM_HUD_USE_TEAM_COLOR = BUILDER
                .comment("Colour each teammate's name using their vanilla scoreboard team colour (the",
                        "same colour /team already assigns), falling back to nameColor below only if",
                        "the team has no colour set.")
                .translation("modernhud.config.team_hud_use_team_color")
                .define("useTeamColor", true);

        TEAM_HUD_NAME_COLOR = BUILDER
                .comment("Fallback teammate name colour when useTeamColor is off, or the team has no",
                        "colour set, as a hex code (e.g. \"E6EEF5\" or \"#E6EEF5\").")
                .translation("modernhud.config.team_hud_name_color")
                .define("nameColor", ColorConfig.toHex(0xE6EEF5));

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enabled = ENABLED.get();
        showHealthBar = SHOW_HEALTH_BAR.get();
        showName = SHOW_NAME.get();
        showHead = SHOW_HEAD.get();

        showHpText = SHOW_HP_TEXT.get();
        hpTextFormat = HP_TEXT_FORMAT.get();
        hpTextPosition = HP_TEXT_POSITION.get();
        lowHealthThresholdPercent = LOW_HEALTH_THRESHOLD_PERCENT.get();
        showVitalsGlow = SHOW_VITALS_GLOW.get();
        vitalsGlowColor = ColorConfig.fromHex(VITALS_GLOW_COLOR.get(), 0xFF3B30);
        hpTextNormalColor = ColorConfig.fromHex(HP_TEXT_NORMAL_COLOR.get(), 0xFFFFFF);
        hpTextLowColor = ColorConfig.fromHex(HP_TEXT_LOW_COLOR.get(), 0xFF5555);

        showArmor = SHOW_ARMOR.get();
        armorTextFormat = ARMOR_TEXT_FORMAT.get();
        armorTextPosition = ARMOR_TEXT_POSITION.get();
        armorTextColor = ColorConfig.fromHex(ARMOR_TEXT_COLOR.get(), 0xC0C0C0);
        position = POSITION.get();
        offsetX = OFFSET_X.get();
        offsetY = OFFSET_Y.get();
        barWidth = BAR_WIDTH.get();
        barHeight = BAR_HEIGHT.get();
        barScale = BAR_SCALE.get();
        segmentCount = SEGMENT_COUNT.get();
        pointsPerSegment = POINTS_PER_SEGMENT.get();
        showOverflowText = SHOW_OVERFLOW_TEXT.get();
        namePosition = NAME_POSITION.get();
        absorptionHeight = ABSORPTION_HEIGHT.get();
        absorptionColorMode = ABSORPTION_COLOR_MODE.get();
        absorptionCustomColor = ColorConfig.fromHex(ABSORPTION_CUSTOM_COLOR.get(), 0xD8D8D0);

        showPanel = SHOW_PANEL.get();
        panelColor = ColorConfig.fromHex(PANEL_COLOR.get(), 0x0A0D11);
        panelOpacity = PANEL_OPACITY.get();
        borderStyle = BORDER_STYLE.get();
        borderColor = ColorConfig.fromHex(BORDER_COLOR.get(), 0xE8ECF0);
        borderOpacity = BORDER_OPACITY.get();
        borderLength = BORDER_LENGTH.get();
        borderOutset = BORDER_OUTSET.get();

        hitSoundVolume = HIT_SOUND_VOLUME.get().floatValue();
        breakSoundVolume = BREAK_SOUND_VOLUME.get().floatValue();
        showFeedbackWhenVictim = SHOW_FEEDBACK_WHEN_VICTIM.get();

        feedbackPosition = FEEDBACK_POSITION.get();
        feedbackOffsetX = FEEDBACK_OFFSET_X.get();
        feedbackOffsetY = FEEDBACK_OFFSET_Y.get();
        feedbackIconWidth = FEEDBACK_ICON_WIDTH.get();
        feedbackIconHeight = FEEDBACK_ICON_HEIGHT.get();
        feedbackIconScale = FEEDBACK_ICON_SCALE.get();

        showHitIcon = SHOW_HIT_ICON.get();
        hitIconDurationMs = HIT_ICON_DURATION_MS.get();
        showBreakIcon = SHOW_BREAK_ICON.get();
        breakIconDurationMs = BREAK_ICON_DURATION_MS.get();

        showTeamHud = SHOW_TEAM_HUD.get();
        teamHudMaxPlayers = TEAM_HUD_MAX_PLAYERS.get();
        teamHudBarWidth = TEAM_HUD_BAR_WIDTH.get();
        teamHudRowGap = TEAM_HUD_ROW_GAP.get();
        teamHudGroupGap = TEAM_HUD_GROUP_GAP.get();
        teamHudShowHead = TEAM_HUD_SHOW_HEAD.get();
        showTeamAbsorption = SHOW_TEAM_ABSORPTION.get();
        teamHudUseTeamColor = TEAM_HUD_USE_TEAM_COLOR.get();
        teamHudNameColor = ColorConfig.fromHex(TEAM_HUD_NAME_COLOR.get(), 0xE6EEF5);
    }

    // --- Runtime setter for the drag-to-place HUD editor (Hit/Break icon only - the player HUD's
    // position is corner-anchored and config-file-only now, same treatment as the weapon widget;
    // see HudEditorScreen's class javadoc for why). Updates the live field AND the backing config
    // value (persisted on save), and forces CUSTOM anchoring so the absolute offset is honoured. ---
    public static void setFeedbackPosition(int x, int y) {
        feedbackPosition = HudPosition.CUSTOM;
        feedbackOffsetX = x;
        feedbackOffsetY = y;
        FEEDBACK_POSITION.set(HudPosition.CUSTOM);
        FEEDBACK_OFFSET_X.set(x);
        FEEDBACK_OFFSET_Y.set(y);
    }

    public static void saveConfig() {
        SPEC.save();
    }
}
