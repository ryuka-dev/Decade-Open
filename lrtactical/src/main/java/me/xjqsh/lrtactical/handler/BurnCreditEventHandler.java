package me.xjqsh.lrtactical.handler;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.util.Igniters;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Burning from a fire a throwable lit is dealt again as the thrower's, the way the throwable's other
 * damage already is: the attacker-less {@code on_fire} hit is cancelled and the same amount dealt with
 * the thrower as its cause. See {@link Igniters}.
 */
@Mod.EventBusSubscriber(modid = EquipmentMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BurnCreditEventHandler {
    /** The entity taking a credited burn right now. Server thread only. */
    private static Entity crediting;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (target.level().isClientSide() || !source.is(DamageTypes.ON_FIRE) || source.getEntity() != null) {
            return;
        }
        Entity owner = Igniters.creditFor(target);
        if (owner == null) {
            return;
        }
        event.setCanceled(true);
        crediting = target;
        try {
            target.hurt(new DamageSource(source.typeHolder(), null, owner), event.getAmount());
        } finally {
            crediting = null;
        }
    }

    /**
     * An attributed hit knocks its target away from the attacker. Burning never pushes anyone, and a
     * push towards wherever the thrower stands would tell the target where that is.
     */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (crediting != null && event.getEntity() == crediting) {
            event.setCanceled(true);
        }
    }
}
