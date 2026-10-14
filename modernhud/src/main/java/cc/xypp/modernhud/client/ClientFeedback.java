package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import cc.xypp.modernhud.config.HudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Client-side reaction to a {@link cc.xypp.modernhud.network.ShieldFeedbackPacket}.
 * Plays the sound locally (so only this one client hears it - never broadcast),
 * and records when to show which feedback icon.
 */
public final class ClientFeedback {

    private static long lastHitTriggerMillis = -1;
    private static long lastBreakTriggerMillis = -1;

    private ClientFeedback() {
    }

    public static void handle(boolean fullyBroken, boolean recipientIsAttacker) {
        if (!recipientIsAttacker && !HudConfig.showFeedbackWhenVictim) {
            // This client is the victim, and they've opted out of seeing/hearing
            // their own hit/break feedback. The attacker's side is never gated
            // by this - only the victim's own client decides this for themselves.
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (fullyBroken) {
            if (player != null) {
                player.playSound(sound("absorption_break"), HudConfig.breakSoundVolume, 1.0f);
            }
            if (recipientIsAttacker) {
                lastBreakTriggerMillis = System.currentTimeMillis();
            }
        } else {
            if (player != null) {
                player.playSound(sound("armor_hit"), HudConfig.hitSoundVolume, 1.0f);
            }
            lastHitTriggerMillis = System.currentTimeMillis();
        }
    }

    /** Decade: the sounds are only in sounds.json, not registered (the server has no such entries). */
    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(ModernHud.MOD_ID, name));
    }

    /** Milliseconds since the last "you broke it" trigger, or -1 if none yet. */
    public static long millisSinceBreakTrigger() {
        return lastBreakTriggerMillis < 0 ? -1 : System.currentTimeMillis() - lastBreakTriggerMillis;
    }

    /** Milliseconds since the last "hit landed on active shield" trigger, or -1 if none yet. */
    public static long millisSinceHitTrigger() {
        return lastHitTriggerMillis < 0 ? -1 : System.currentTimeMillis() - lastHitTriggerMillis;
    }
}
