package com.tacz.guns.restriction;

import com.tacz.guns.GunMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Decade: the client's copy of what the server last said through {@link GunUseRestriction}. Asked by
 * the shoot, reload and melee inputs before they lock state or play anything; a prediction only, the
 * server refuses the same actions on its side. Written on the client thread, cleared on logout.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class ClientGunUseRestriction {
    private static int usableSlots = GunUseRestriction.ALL_SLOTS;
    private static boolean attackSuspended;
    private static boolean reloadSuspended;

    private ClientGunUseRestriction() {
    }

    static void acceptSlots(int received) {
        usableSlots = received;
    }

    static void acceptSuspended(boolean attack, boolean reload) {
        attackSuspended = attack;
        reloadSuspended = reload;
    }

    /** Shooting and melee hits with the gun in the main hand. */
    public static boolean blocksAttack(LocalPlayer player) {
        return attackSuspended || outsideSlots(player);
    }

    public static boolean blocksReload(LocalPlayer player) {
        return reloadSuspended || outsideSlots(player);
    }

    /** How many hotbar slots, from the first, guns work in; {@link GunUseRestriction#ALL_SLOTS} when not told. */
    public static int usableSlots() {
        return usableSlots;
    }

    private static boolean outsideSlots(LocalPlayer player) {
        return player.getInventory().selected >= usableSlots;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        usableSlots = GunUseRestriction.ALL_SLOTS;
        attackSuspended = false;
        reloadSuspended = false;
    }
}
