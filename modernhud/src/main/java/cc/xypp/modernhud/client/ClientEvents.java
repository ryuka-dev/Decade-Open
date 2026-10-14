package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.WeaponConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;

import java.util.function.IntSupplier;

/**
 * Registers the client overlays:
 *  - the personal HUD widget (head/name/absorption bar/health bar/armor bar)
 *  - the team HUD (vanilla-scoreboard-team teammate health list) - registered right after the
 *    personal widget so it renders after it every frame, since it reads that widget's
 *    last-computed screen position/size to stack itself directly above it
 *  - the hit/break feedback icon
 *  - the TACZ weapon widget (soft dependency - see WeaponHudOverlay/TaczCompat)
 *  - the TACZ ammo alert (RELOAD / NO AMMO / LOW AMMO), same soft dependency
 *
 * The actual "did this hit land on/break Absorption" detection lives
 * server-side in CommonEvents, since it needs to work for every entity,
 * not just you, and needs to know who the attacker was.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterOverlays(final RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("player_hud", new PlayerHudOverlay());
        event.registerAboveAll("team_hud", new TeamHudOverlay());
        event.registerAboveAll("feedback_icon", new FeedbackIconOverlay());
        // Safe to always register: render() itself no-ops instantly (TaczCompat.loaded() check)
        // on a TACZ-free install, and its overlay signature references no TACZ types.
        event.registerAboveAll("weapon_hud", new WeaponHudOverlay());
        event.registerAboveAll("ammo_alert", new AmmoAlertOverlay());
    }

    /**
     * Decade: tells Decade's TaCZ build how much of the bottom-right corner the weapon widget takes,
     * so its subtitle offset moves the subtitles above this widget too. A plain {@link IntSupplier},
     * no class of either mod crosses; an upstream TaCZ ignores the message.
     */
    @SubscribeEvent
    public static void onInterModEnqueue(final InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("tacz")) {
            IntSupplier height = WeaponHudOverlay::reservedHeight;
            InterModComms.sendTo("tacz", "hud_bottom_right_height", () -> height);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(WeaponConfig::syncHudSource);
    }
}
