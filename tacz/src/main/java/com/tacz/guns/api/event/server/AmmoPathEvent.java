package com.tacz.guns.api.event.server;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/**
 * Decade: the stretch a bullet actually flew in one tick, on the server — from where it started the tick
 * to where it stopped: the end of its move, the block it hit, or the last entity it went into before it
 * was spent. Posted once per bullet per tick, after the hits of that tick are dealt.
 *
 * <p>Outside the bullet this cannot be seen: a bullet that hits something is discarded before its position
 * moves, so sampling its position each tick loses the last stretch — which is where a near miss that ends
 * in the wall behind someone happens. Only for listening; nothing here changes the bullet.
 */
public class AmmoPathEvent extends Event {
    private final EntityKineticBullet ammo;
    private final Vec3 from;
    private final Vec3 to;

    public AmmoPathEvent(EntityKineticBullet ammo, Vec3 from, Vec3 to) {
        this.ammo = ammo;
        this.from = from;
        this.to = to;
    }

    public EntityKineticBullet getAmmo() {
        return ammo;
    }

    public Vec3 getFrom() {
        return from;
    }

    public Vec3 getTo() {
        return to;
    }
}
