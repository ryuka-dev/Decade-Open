package com.tacz.guns.config.client;

import net.minecraftforge.common.ForgeConfigSpec;

public class KeyConfig {
    public static ForgeConfigSpec.BooleanValue HOLD_TO_AIM;
    public static ForgeConfigSpec.BooleanValue HOLD_TO_CRAWL;
    public static ForgeConfigSpec.BooleanValue AUTO_RELOAD;
    public static ForgeConfigSpec.BooleanValue AUTO_UNJAM;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("key");

        builder.comment("True if you want to hold the right mouse button to aim");
        HOLD_TO_AIM = builder.define("HoldToAim", true);

        builder.comment("True if you want to hold the crawl button to crawl");
        HOLD_TO_CRAWL = builder.define("HoldToCrawl", true);

        builder.comment("Try to reload automatically when the gun is empty");
        AUTO_RELOAD = builder.define("AutoReload", false);

        // Decade: clears a jammed gun without the player pressing inspect (AutoUnjam, README-DECADE.md)
        builder.comment("Clear a jammed gun automatically, as if you had pressed inspect. Guns only jam when another mod makes them");
        AUTO_UNJAM = builder.define("AutoUnjam", true);

        builder.pop();
    }
}
