package cc.xypp.modernhud.config;

/** How a panel chip's edge is decorated, on top of its flat fill (see {@code HudPanel#draw}). */
public enum PanelBorderStyle {
    /** No border/accent at all - just the flat fill (or nothing, if the fill is also off). */
    NONE,
    /** Four short two-segment "L" marks, one per corner, edges between them left open - the MW4-style
     *  tech-HUD accent (see {@code HudPanel#drawCornerBrackets}). */
    CORNERS,
    /** A complete thin (1px) outline around all four edges. */
    FULL
}
