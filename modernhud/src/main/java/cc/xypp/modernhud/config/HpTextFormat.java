package cc.xypp.modernhud.config;

/** How the HP number is formatted. */
public enum HpTextFormat {
    /** e.g. "20/20" */
    CLASSIC,
    /** Decade: e.g. "48/60" - whole points, for a 60-point health where tenths say nothing */
    WHOLE,
    /** e.g. "100%" */
    PERCENT
}
