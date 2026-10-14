package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.gui.overlay.WeaponSlotHudOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

import java.util.function.IntSupplier;

/**
 * Decade: another mod that draws in the bottom-right corner (a weapon HUD replacing this one) can say how
 * far up from the bottom of the screen it reaches, so the subtitles move above it as they do above
 * {@link WeaponSlotHudOverlay}. One message {@code hud_bottom_right_height} to {@code tacz}, carrying a
 * supplier of a plain {@link IntSupplier} read every frame (0 when that mod draws nothing). The last one wins.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class HudHeightMessages {
    public static final String HEIGHT = "hud_bottom_right_height";

    private HudHeightMessages() {
    }

    @SubscribeEvent
    public static void onInterModProcess(InterModProcessEvent event) {
        InterModComms.getMessages(GunMod.MOD_ID, HEIGHT::equals).forEach(message -> {
            Object payload = message.messageSupplier().get();
            if (payload instanceof IntSupplier height) {
                WeaponSlotHudOverlay.reserveBottomRight(height);
                GunMod.LOGGER.info("Bottom-right HUD height registered by inter-mod message from {}", message.senderModId());
            } else {
                GunMod.LOGGER.error("{} from {} is not an IntSupplier, ignored", HEIGHT, message.senderModId());
            }
        });
    }
}
