package me.xjqsh.lrtactical.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Who set an entity alight with a throwable. Vanilla burning ({@code minecraft:on_fire}) names no
 * attacker, so without this a molotov's burn belongs to nobody: no kill credit, no damage attributed to
 * the thrower, and anything that judges damage by who dealt it sees the environment instead.
 *
 * <p>An ignition is credited for as long as the fire it lit is due to burn, extended when the flammable
 * effect keeps that fire going. A fire lit afterwards by something else (lava, a fire block) is credited
 * only if it starts before that runs out.
 *
 * <p>Server thread only. Entities are held weakly, so one that goes away takes its entry with it.
 */
public final class Igniters {
    private record Ignition(UUID owner, long until) {
    }

    private static final Map<Entity, Ignition> IGNITIONS = new WeakHashMap<>();

    private Igniters() {
    }

    /** Sets {@code target} on fire as {@link Entity#setSecondsOnFire} does, and remembers {@code owner} lit it. */
    public static void ignite(Entity target, @Nullable Entity owner, int seconds) {
        target.setSecondsOnFire(seconds);
        if (owner == null || owner == target || target.level().isClientSide() || !target.isOnFire()) {
            return;
        }
        long now = target.level().getGameTime();
        IGNITIONS.values().removeIf(i -> i.until() < now);
        IGNITIONS.put(target, new Ignition(owner.getUUID(), now + target.getRemainingFireTicks()));
    }

    /** The fire on {@code target} was kept going; a credited ignition lasts as long as it now does. */
    public static void extend(Entity target) {
        Ignition ignition = IGNITIONS.get(target);
        if (ignition == null) {
            return;
        }
        long until = target.level().getGameTime() + target.getRemainingFireTicks();
        if (until > ignition.until()) {
            IGNITIONS.put(target, new Ignition(ignition.owner(), until));
        }
    }

    /** Who lit the fire {@code target} is burning in, if a throwable did and they are still in its level. */
    @Nullable
    public static Entity creditFor(Entity target) {
        Ignition ignition = IGNITIONS.get(target);
        if (ignition == null) {
            return null;
        }
        if (!target.isOnFire() || ignition.until() < target.level().getGameTime()) {
            IGNITIONS.remove(target);
            return null;
        }
        return target.level() instanceof ServerLevel level ? level.getEntity(ignition.owner()) : null;
    }
}
