package me.xjqsh.lrtactical.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.xjqsh.lrtactical.api.LrTacticalAPI;
import me.xjqsh.lrtactical.api.item.IConsumable;
import me.xjqsh.lrtactical.client.input.ItemUseButtons;
import me.xjqsh.lrtactical.config.UseButtons;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * The attack and use keys for consumables, throwables and the detonator, as {@link ItemUseButtons}
 * sets them; cancel presses are taken in {@code ConsumableInputHandler}.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow @Final public Options options;
    @Shadow public LocalPlayer player;
    @Shadow public MultiPlayerGameMode gameMode;
    @Shadow @Final public GameRenderer gameRenderer;
    @Shadow private int rightClickDelay;

    @WrapWithCondition(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"
            )
    )
    private boolean lrtactical$shouldReleaseUsingItem(MultiPlayerGameMode gameMode, Player player) {
        if (player == null || !player.isUsingItem()) {
            return true;
        }

        ItemStack useItem = player.getUseItem();
        if (!(useItem.getItem() instanceof IConsumable)) {
            return true;
        }

        return LrTacticalAPI.getConsumableIndex(useItem)
                .map(index -> !index.getData().isToggleUse())
                .orElse(true);
    }

    /** Vanilla's "is the use key still held" while using: here, whichever buttons hold this use. */
    @WrapOperation(
            method = "handleKeybinds",
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V")
            ),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z")
    )
    private boolean lrtactical$keepsUsing(KeyMapping useKey, Operation<Boolean> original) {
        Boolean keeps = ItemUseButtons.keepsUsing(this.player, this.options);
        return keeps != null ? keeps : original.call(useKey);
    }

    /** A click of the attack key: the item's use if the left button uses it, otherwise nothing. */
    @WrapOperation(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startAttack()Z")
    )
    private boolean lrtactical$attackClick(Minecraft minecraft, Operation<Boolean> original) {
        UseButtons buttons = ItemUseButtons.ofMainHand(this.player);
        if (buttons == null) {
            return original.call(minecraft);
        }
        if (buttons.uses(true)) {
            lrtactical$useMainHandItem();
        }
        return false;
    }

    /** A click of the use key: vanilla if the right button uses the item, otherwise nothing. */
    @WrapOperation(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 0)
    )
    private void lrtactical$useClick(Minecraft minecraft, Operation<Void> original) {
        UseButtons buttons = ItemUseButtons.ofMainHand(this.player);
        if (buttons == null || buttons.uses(false)) {
            original.call(minecraft);
        }
    }

    /** The use key held: repeats as vanilla does, unless it does not use the item or a cancel is pending. */
    @WrapOperation(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 1)
    )
    private void lrtactical$useHeld(Minecraft minecraft, Operation<Void> original) {
        UseButtons buttons = ItemUseButtons.ofMainHand(this.player);
        if (buttons == null || buttons.uses(false) && ItemUseButtons.mayRepeat()) {
            original.call(minecraft);
        }
    }

    /**
     * The attack key held: repeats the use as holding the use key does, on the same delay, if the
     * left button uses the item. Never digs with these items.
     */
    @WrapOperation(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V")
    )
    private void lrtactical$attackHeld(Minecraft minecraft, boolean leftClick, Operation<Void> original) {
        UseButtons buttons = ItemUseButtons.ofMainHand(this.player);
        if (buttons == null) {
            original.call(minecraft, leftClick);
            return;
        }
        if (leftClick && buttons.uses(true) && ItemUseButtons.mayRepeat()
                && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
            lrtactical$useMainHandItem();
        }
        original.call(minecraft, false);
    }

    /**
     * The item half of vanilla's startUseItem, main hand only. The block and entity half is left
     * out: the attack key does not open doors or talk to villagers.
     */
    @Unique
    private void lrtactical$useMainHandItem() {
        if (this.gameMode.isDestroying()) {
            return;
        }
        this.rightClickDelay = 4;
        if (this.player.isHandsBusy()) {
            return;
        }
        // announced as the use key, so whatever may veto a right click vetoes this too
        var event = ForgeHooksClient.onClickInput(1, this.options.keyUse, InteractionHand.MAIN_HAND);
        if (event.isCanceled()) {
            if (event.shouldSwingHand()) {
                this.player.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }
        InteractionResult result = this.gameMode.useItem(this.player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            if (result.shouldSwing() && event.shouldSwingHand()) {
                this.player.swing(InteractionHand.MAIN_HAND);
            }
            this.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);
        }
    }
}
