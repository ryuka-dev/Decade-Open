package com.tacz.guns.client.input;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
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
 * Decade: a cleared jam can fire at once, not a round trip later, and firing cuts the last 200 ms of the
 * clearing short, as it does the end of a reload (README-DECADE.md).
 *
 * <p>GunDB (Gun Durability) sends its unjam packet the moment its clearing animation ends, and only clears the
 * {@code Jammed} tag on the client when the server answers; until then its client-side check turns every shot
 * into a dry fire. So the gun stood idle for the player's ping plus up to a server tick after every jam
 * (measured 100 to 150 ms at 40 to 90 ms ping). This clears the client's copy of the tag as soon as the
 * animation has ended. The server needs nothing: the unjam packet was sent before any shot that follows,
 * over the same connection, and both are run on the server thread in the order they arrived.
 *
 * <p>"Ended" is the clearing animation stopping while the gun stays in the same slot. GunDB's clearing state
 * leaves on one input only, the one that sends the packet, and stops the animation as it leaves. The state
 * machine also runs that exit when the gun is put away, without sending anything, so a change of slot drops
 * the prediction (two guns of one model share a state machine, so the item alone would not tell). A gun with
 * no clearing animation is cleared through inspect and a server timer, and is not predicted.
 *
 * <p>A reload lets the gun fire before its animation ends (our reload numbers keep that point 200 ms or more
 * before the end), so pulling the trigger cuts the gun's return to the shoulder short. GunDB's clearing
 * allows no such thing: the gun fires only once the whole animation has played. So with the trigger held in
 * the last {@link #EARLY_FIRE_NS} of it, this gives GunDB's state machine the input it gives itself at the
 * end ({@code unjam_finished}): GunDB then sends its packet and leaves the clearing state exactly as if the
 * animation had run out. Applies whether the jam was cleared by hand or by
 * {@link AutoUnjam}, and needs no GunDB class: the tag and the animation are read by name.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class UnjamPrediction {
    private static final String JAMMED_TAG = "Jammed";
    private static final String UNJAM_ANIMATION = "unjam";
    private static final int TRACKS = 16;
    private static final String UNJAM_FINISHED_INPUT = "unjam_finished";
    private static final long EARLY_FIRE_NS = 200_000_000L;

    /** The clearing animation seen running last tick, or null, and the slot it was seen in. */
    private static ObjectAnimationRunner clearing;
    private static int clearingSlot;

    private UnjamPrediction() {
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
        ItemStack stack = player.getMainHandItem();
        CompoundTag tag = stack.getTag();
        if (!(stack.getItem() instanceof IGun) || tag == null || !tag.getBoolean(JAMMED_TAG)) {
            clearing = null;
            return;
        }
        LuaAnimationStateMachine<?> machine = TimelessAPI.getGunDisplay(stack)
                .map(GunDisplayInstance::getAnimationStateMachine).orElse(null);
        if (machine == null || !machine.isInitialized()) {
            clearing = null;
            return;
        }
        int slot = player.getInventory().selected;
        if (clearing != null && clearingSlot == slot && clearing.isStopped()
                && isOnATrack(machine.getAnimationController(), clearing)) {
            tag.putBoolean(JAMMED_TAG, false);
            clearing = null;
            return;
        }
        clearing = findClearing(machine.getAnimationController());
        clearingSlot = slot;
        if (clearing != null && ShootKey.SHOOT_KEY.isDown() && remainingNs(clearing) <= EARLY_FIRE_NS) {
            machine.trigger(UNJAM_FINISHED_INPUT);
            // GunDB stops the animation as it leaves the state; clear now so the trigger fires this very tick
            if (clearing.isStopped()) {
                tag.putBoolean(JAMMED_TAG, false);
                clearing = null;
            }
        }
    }

    private static long remainingNs(ObjectAnimationRunner runner) {
        return (long) (runner.getAnimation().getMaxEndTimeS() * 1e9) - runner.getProgressNs();
    }

    private static ObjectAnimationRunner findClearing(AnimationController controller) {
        for (int track = 0; track < TRACKS; track++) {
            ObjectAnimationRunner runner = controller.getAnimation(track);
            if (runner != null && runner.getTransitionTo() != null) {
                runner = runner.getTransitionTo();
            }
            if (runner != null && UNJAM_ANIMATION.equals(runner.getAnimation().name) && !runner.isStopped()) {
                return runner;
            }
        }
        return null;
    }

    private static boolean isOnATrack(AnimationController controller, ObjectAnimationRunner runner) {
        for (int track = 0; track < TRACKS; track++) {
            ObjectAnimationRunner current = controller.getAnimation(track);
            if (current == runner || current != null && current.getTransitionTo() == runner) {
                return true;
            }
        }
        return false;
    }
}
