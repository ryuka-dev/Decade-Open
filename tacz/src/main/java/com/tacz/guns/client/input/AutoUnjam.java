package com.tacz.guns.client.input;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.config.client.KeyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * Decade: clears a jammed gun on its own, as {@code AutoReload} reloads an empty one (README-DECADE.md).
 *
 * <p>Jamming is not TaCZ's. Gun Durability (GunDB) tags a jammed gun {@code Jammed} and clears it when the
 * player inspects that gun: it hooks both inspect entry points of {@link InspectKey}, ahead of TaCZ's own
 * inspect. This only presses inspect for the player, through the controller entry point, and reads the tag
 * by name; nothing of GunDB is referenced. Without GunDB no gun is ever tagged and this never acts.
 *
 * <p>One press per jam: GunDB plays the clearing sound on every press, before it asks the gun's animation
 * state machine, and the slowest gun's clearing animation runs about 9 seconds. So nothing is pressed until
 * that state machine is set up, which happens when the gun is first drawn on screen, a frame or more after
 * the slot changes: a press before that is dropped. GunDB's clearing state takes the client state lock as
 * it starts, inside the press, so a press that left the lock open was dropped too (the gun was
 * mid-inspect) and is repeated after {@link #DROPPED_RETRY_TICKS}. A gun with no clearing animation is
 * cleared by GunDB with its inspect animation and a timer on the server, which takes no lock; it is pressed
 * again only after {@link #RETRY_TICKS}. Putting the gun away and drawing it again counts as a new jam.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class AutoUnjam {
    private static final String JAMMED_TAG = "Jammed";
    private static final String UNJAM_ANIMATION = "unjam";
    private static final int RETRY_TICKS = 200;
    private static final int DROPPED_RETRY_TICKS = 20;

    private static LocalPlayer pressedBy;
    private static int pressedSlot = -1;
    private static int pressedAt;
    private static int retryTicks;

    private AutoUnjam() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != LogicalSide.CLIENT) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.player != player) {
            return;
        }
        if (player.isSpectator() || !KeyConfig.AUTO_UNJAM.get() || !holdsJammedGun(player)) {
            pressedBy = null;
            return;
        }
        int slot = player.getInventory().selected;
        if (pressedBy == player && pressedSlot == slot && player.tickCount - pressedAt < retryTicks) {
            return;
        }
        // the moments a manual inspect would be refused or cut short
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        LocalPlayerDataHolder data = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
        if (operator.getSynReloadState().getStateType().isReloading() || operator.getSynDrawCoolDown() != 0
                || operator.getSynIsBolting() || operator.getSynMeleeCoolDown() != 0 || data.clientStateLock) {
            return;
        }
        LuaAnimationStateMachine<?> machine = TimelessAPI.getGunDisplay(player.getMainHandItem())
                .map(GunDisplayInstance::getAnimationStateMachine).orElse(null);
        if (machine == null || !machine.isInitialized()) {
            return;
        }
        boolean animated = machine.getAnimationController().containPrototype(UNJAM_ANIMATION);
        // false while a screen is open: nothing was pressed, so it is tried again next tick
        if (InspectKey.onInspectControllerPress(true)) {
            pressedBy = player;
            pressedSlot = slot;
            pressedAt = player.tickCount;
            retryTicks = animated && !data.clientStateLock ? DROPPED_RETRY_TICKS : RETRY_TICKS;
        }
    }

    private static boolean holdsJammedGun(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        CompoundTag tag = stack.getTag();
        return stack.getItem() instanceof IGun && tag != null && tag.getBoolean(JAMMED_TAG);
    }
}
