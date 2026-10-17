package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Decade: another mod can add what the interact key reaches while a gun is held, beyond the configured lists: a block
 * by where it is ({@code interact_block_rule}, a {@code BiPredicate<BlockPos, BlockState>}), an entity by what it is
 * ({@code interact_entity_rule}, a {@code Predicate<Entity>}). Each message carries a supplier of the rule; the rules
 * are asked on the client every frame for the prompt and on each press, after the blacklists, so a blacklist still
 * wins. Every rule received is kept.
 * <p>And what it never reaches (decade.17): {@code interact_block_veto} and {@code interact_entity_veto}, the same
 * types; asked right after the blacklists, before any rule or whitelist.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class InteractRuleMessages {
    public static final String BLOCK = "interact_block_rule";
    public static final String ENTITY = "interact_entity_rule";
    public static final String BLOCK_VETO = "interact_block_veto";
    public static final String ENTITY_VETO = "interact_entity_veto";

    private InteractRuleMessages() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onInterModProcess(InterModProcessEvent event) {
        InterModComms.getMessages(GunMod.MOD_ID, m -> m.equals(BLOCK) || m.equals(ENTITY) || m.equals(BLOCK_VETO)
                || m.equals(ENTITY_VETO)).forEach(message -> {
            Object payload = message.messageSupplier().get();
            if (message.method().equals(BLOCK) && payload instanceof BiPredicate<?, ?> rule) {
                InteractKeyConfigRead.addBlockRule((BiPredicate<BlockPos, BlockState>) rule);
            } else if (message.method().equals(ENTITY) && payload instanceof Predicate<?> rule) {
                InteractKeyConfigRead.addEntityRule((Predicate<Entity>) rule);
            } else if (message.method().equals(BLOCK_VETO) && payload instanceof BiPredicate<?, ?> veto) {
                InteractKeyConfigRead.addBlockVeto((BiPredicate<BlockPos, BlockState>) veto);
            } else if (message.method().equals(ENTITY_VETO) && payload instanceof Predicate<?> veto) {
                InteractKeyConfigRead.addEntityVeto((Predicate<Entity>) veto);
            } else {
                GunMod.LOGGER.error("{} from {} is not the rule it should carry, ignored", message.method(), message.senderModId());
                return;
            }
            GunMod.LOGGER.info("Interact key rule {} registered by inter-mod message from {}", message.method(), message.senderModId());
        });
    }
}
