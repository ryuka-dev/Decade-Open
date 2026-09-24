package me.xjqsh.lrtactical.api.animation;

import com.tacz.guns.client.animation.statemachine.ItemAnimationStateContext;
import net.minecraft.world.item.ItemStack;

public class ConsumableAnimationStateContext extends ItemAnimationStateContext {
    private ItemStack currentItem = ItemStack.EMPTY;
    private boolean using = false;
    private int usingTick = 0;
    private boolean held = true;

    public void setCurrentItem(ItemStack currentItem) {
        this.currentItem = currentItem;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public int getStackCount() {
        return currentItem.getCount();
    }

    public boolean isUsing() {
        return using;
    }

    public void setUsing(boolean using) {
        this.using = using;
    }

    public int getUsingTick() {
        return usingTick;
    }

    public void setUsingTick(int usingTick) {
        this.usingTick = usingTick;
    }

    /**
     * Whether this item is still what the player holds: the slot it was drawn from is still
     * selected, and that slot still has this consumable. False between switching away and the
     * put-away, which comes a tick later.
     */
    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

}
