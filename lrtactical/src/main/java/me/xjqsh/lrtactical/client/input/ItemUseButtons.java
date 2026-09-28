package me.xjqsh.lrtactical.client.input;

import com.tacz.guns.client.input.InteractKey;
import me.xjqsh.lrtactical.api.LrTacticalAPI;
import me.xjqsh.lrtactical.api.item.IConsumable;
import me.xjqsh.lrtactical.api.item.IThrowable;
import me.xjqsh.lrtactical.config.ClientConfig;
import me.xjqsh.lrtactical.config.UseButtons;
import me.xjqsh.lrtactical.item.DetonatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Which mouse buttons use consumables, throwables and the detonator, as the player set it (TaCZ guns
 * give both buttons a job; before, the attack key did what an empty hand does). Click uses (toggle-mode
 * consumables, the detonator) and hold uses (hold-mode consumables, throwables) are set apart. The
 * keys are read in {@code MinecraftMixin}, and the cancel presses in {@link ConsumableInputHandler}.
 * Holding the interact key leaves both buttons to vanilla, as {@code ClientEventsHandler} does.
 * Client input only: the server sees the same packets a right click sends.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class ItemUseButtons {
    /**
     * Set by a cancel while a button that starts the use is still held: holding it on would start
     * the use again at once. Cleared once both buttons are up.
     */
    private static boolean awaitingRelease;

    private ItemUseButtons() {
    }

    /** The buttons for this item, or null if it is none of these items. */
    @Nullable
    public static UseButtons of(ItemStack stack) {
        if (stack.getItem() instanceof IConsumable) {
            return LrTacticalAPI.getConsumableIndex(stack)
                    .map(index -> index.getData().isToggleUse() ? ClientConfig.CLICK_USE_BUTTONS.get() : ClientConfig.HOLD_USE_BUTTONS.get())
                    .orElse(null);
        }
        if (stack.getItem() instanceof IThrowable) {
            return ClientConfig.HOLD_USE_BUTTONS.get();
        }
        if (stack.getItem() instanceof DetonatorItem) {
            return ClientConfig.CLICK_USE_BUTTONS.get();
        }
        return null;
    }

    /** The buttons for the main-hand item, or null when vanilla keeps both buttons. */
    @Nullable
    public static UseButtons ofMainHand(LocalPlayer player) {
        if (InteractKey.INTERACT_KEY.isDown()) {
            return null;
        }
        return of(player.getMainHandItem());
    }

    private static boolean isToggle(ItemStack stack) {
        return stack.getItem() instanceof IConsumable && LrTacticalAPI.getConsumableIndex(stack)
                .map(index -> index.getData().isToggleUse())
                .orElse(false);
    }

    /** The main-hand use under way, if it is one of these items, else null. */
    @Nullable
    private static ItemStack currentUse(LocalPlayer player) {
        if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return null;
        }
        ItemStack useItem = player.getUseItem();
        return of(useItem) == null ? null : useItem;
    }

    /**
     * Whether the use under way carries on, in place of vanilla's "the use key is held"; null for a
     * use that is not one of these items. A toggle use carries on until finished or cancelled.
     */
    @Nullable
    public static Boolean keepsUsing(LocalPlayer player, Options options) {
        ItemStack useItem = currentUse(player);
        if (useItem == null) {
            return null;
        }
        if (isToggle(useItem)) {
            return true;
        }
        UseButtons buttons = of(useItem);
        return buttons.uses(true) && options.keyAttack.isDown() || buttons.uses(false) && options.keyUse.isDown();
    }

    /**
     * Whether pressing this button cancels the use under way. A toggle use: any cancel button. A
     * hold use: a cancel button that does not also hold it; letting go of those ends the use instead.
     */
    public static boolean cancelsCurrentUse(LocalPlayer player, boolean left) {
        ItemStack useItem = currentUse(player);
        if (useItem == null) {
            return false;
        }
        UseButtons buttons = of(useItem);
        return buttons.cancels(left) && (isToggle(useItem) || !buttons.uses(left));
    }

    /** The overlay's line for the buttons that cancel this use, or null when none does. */
    @Nullable
    public static String cancelHintKey(ItemStack useItem) {
        UseButtons buttons = of(useItem);
        if (buttons == null) {
            return null;
        }
        boolean toggle = isToggle(useItem);
        boolean left = buttons.cancels(true) && (toggle || !buttons.uses(true));
        boolean right = buttons.cancels(false) && (toggle || !buttons.uses(false));
        if (left && right) {
            return "overlay.lrtactical.consumable.toggle_hint.both";
        }
        if (left) {
            return "overlay.lrtactical.consumable.toggle_hint.left";
        }
        return right ? "overlay.lrtactical.consumable.toggle_hint" : null;
    }

    public static void awaitRelease() {
        awaitingRelease = true;
    }

    /** Whether holding a button may start the use again. */
    public static boolean mayRepeat() {
        return !awaitingRelease;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !awaitingRelease) {
            return;
        }
        Options options = Minecraft.getInstance().options;
        if (!options.keyAttack.isDown() && !options.keyUse.isDown()) {
            awaitingRelease = false;
        }
    }
}
