package cc.xypp.modernhud;

import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.WeaponConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Modern HUD
 * -----------
 * A lightweight, FPS-shooter-inspired HUD overhaul built entirely on vanilla
 * mechanics - no new items, blocks, or ores anywhere in this mod.
 *
 * Two independent pieces, each fully toggleable/positionable on its own:
 *
 *  1. Personal status widget (client-only): head, name, a segmented
 *     Absorption bar, a health bar, an optional Armor bar, and an HP
 *     readout (classic or percent, true decimal precision).
 *  2. Private hit/break feedback (needs the mod on the server too to work
 *     for entities other than yourself): a hit landing on active Absorption
 *     plays a sound for the attacker and victim ONLY (never broadcast to
 *     anyone else nearby); fully depleting it also flashes a positionable
 *     icon for the attacker.
 *  3. A TACZ weapon widget (client-only, soft dependency): the held gun's
 *     own HUD icon, fire-mode icons (shown only for guns with more than one
 *     mode), and mag/reserve ammo as plain numbers (no gauge bar), plus a
 *     small secondary-weapon slot for the last different gun you were
 *     holding, and a pulsing centre-screen RELOAD/NO AMMO/LOW AMMO alert.
 *     Entirely inert when TACZ isn't installed - and when it IS installed,
 *     TACZ's own built-in gun HUD is turned off in favor of this one (see
 *     TaczCompat#setVanillaGunHudEnabled) so the two don't render on top of
 *     each other. A keybind (unbound by default) lets you alternate live
 *     between this widget and TACZ's own instead, if you'd rather.
 *  4. A team HUD (client-only): one compact row per vanilla scoreboard
 *     teammate - head, name, an Absorption bar, and a health bar, all
 *     smoothly animated - stacked above the personal widget. A teammate in
 *     Spectator mode always shows an empty Absorption bar, never a stale
 *     leftover value.
 *
 * The server-side piece (#2) is purely event detection + a cosmetic
 * sound/packet - nothing here changes gameplay.
 *
 * This mod does not include a compass or gunfire-direction indicator -
 * that's intentionally left to other mods that already do it.
 */
@Mod(ModernHud.MOD_ID)
public class ModernHud {

    public static final String MOD_ID = "modernhud";

    public ModernHud() {
        // Decade: client-only. The upstream server half (CommonEvents, its network channel and
        // common config) is removed - the server does not carry this mod, and the plate hit/break
        // sounds are played by Decade's own server mod by name (see README-DECADE.md).

        // Client-only configs and keybinds. Each needs its own explicit file
        // name - otherwise Forge derives "<modid>-<type>.toml" for both,
        // which collide since they're both CLIENT type under the same modid.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC, "modernhud-hud.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WeaponConfig.SPEC, "modernhud-weapon.toml");
        }
    }
}
