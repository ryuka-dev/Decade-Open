package com.tacz.guns.restriction;

import com.tacz.guns.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Decade: what a server tells a client about where and when its guns may not be used, so the client
 * plays nothing for a shot, reload or melee hit the server is going to refuse. The rules themselves
 * stay with the server, which also refuses those actions on its side; this only carries their outcome.
 * See README-DECADE.md.
 *
 * <p>Nothing is kept here: each call is sent as it is. A client that has been told nothing restricts
 * nothing, and forgets everything when it leaves the server.
 */
public final class GunUseRestriction {
    /** Every hotbar slot: no restriction by slot. */
    public static final int ALL_SLOTS = 9;

    private GunUseRestriction() {
    }

    /** Guns may be used only from the first {@code usableSlots} hotbar slots (1 to {@link #ALL_SLOTS}). */
    public static void sendSlots(ServerPlayer player, int usableSlots) {
        if (usableSlots < 1 || usableSlots > ALL_SLOTS) {
            throw new IllegalArgumentException("usable hotbar slots out of range: " + usableSlots);
        }
        send(player, ServerMessageGunUseRestriction.slots(usableSlots));
    }

    /**
     * Whether the player may, at the moment, attack with a gun (shoot or melee) and reload one. Sent
     * when either changes; both false lifts the suspension.
     */
    public static void sendSuspended(ServerPlayer player, boolean attack, boolean reload) {
        send(player, ServerMessageGunUseRestriction.suspended(attack, reload));
    }

    private static void send(ServerPlayer player, ServerMessageGunUseRestriction message) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
