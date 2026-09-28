package me.xjqsh.lrtactical.client.input;

import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.network.NetworkHandler;
import me.xjqsh.lrtactical.network.message.CCancelItemUse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Cancels a use on the press of a cancel button ({@link ItemUseButtons}). Taken at the press itself:
 * while an item is in use vanilla throws the clicks of both buttons away unread.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = EquipmentMod.MOD_ID)
public class ConsumableInputHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.keyUse.matches(event.getKey(), event.getScanCode())) {
                tryCancelUse(mc, false);
            } else if (mc.options.keyAttack.matches(event.getKey(), event.getScanCode())) {
                tryCancelUse(mc, true);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.keyUse.matchesMouse(event.getButton()) && tryCancelUse(mc, false)
                    || mc.options.keyAttack.matchesMouse(event.getButton()) && tryCancelUse(mc, true)) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean tryCancelUse(Minecraft mc, boolean left) {
        LocalPlayer player = mc.player;
        // a click inside a screen is the screen's
        if (player == null || player.isSpectator() || mc.screen != null
                || !ItemUseButtons.cancelsCurrentUse(player, left)) {
            return false;
        }

        // stops without finishing: no effects, nothing thrown
        player.stopUsingItem();
        NetworkHandler.sendToServer(new CCancelItemUse());
        ItemUseButtons.awaitRelease();
        while (mc.options.keyUse.consumeClick()) {
        }
        while (mc.options.keyAttack.consumeClick()) {
        }
        return true;
    }
}
