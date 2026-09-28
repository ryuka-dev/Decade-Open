package me.xjqsh.lrtactical.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static ForgeConfigSpec.BooleanValue BLACK_FLASH;
    public static ForgeConfigSpec.DoubleValue EXPLODE_SCREEN_SHAKE_MULTIPLIER;
    public static ForgeConfigSpec.EnumValue<UseButtons> CLICK_USE_BUTTONS;
    public static ForgeConfigSpec.EnumValue<UseButtons> HOLD_USE_BUTTONS;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Use black overlay instead of white when blinded by flashbang");
        BLACK_FLASH = builder.define("blackFlash", false);
        EXPLODE_SCREEN_SHAKE_MULTIPLIER = builder
                .comment("Screen shake multiplier for explosions, default is 1.0")
                .defineInRange("explodeScreenShakeMultiplier", 1.0, 0.0, 128.0);
        CLICK_USE_BUTTONS = builder
                .comment("Mouse buttons that start and cancel click uses: toggle-mode consumables and the detonator")
                .defineEnum("clickUseButtons", UseButtons.BOTH);
        HOLD_USE_BUTTONS = builder
                .comment("Mouse buttons that start and cancel hold uses: hold-mode consumables and throwables")
                .defineEnum("holdUseButtons", UseButtons.BOTH);
        return builder.build();
    }
}
