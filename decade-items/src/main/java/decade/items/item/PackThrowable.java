package decade.items.item;

import me.xjqsh.lrtactical.item.ThrowableItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A throwable run by lrtactical but registered as our own item, the throwable twin of {@link PackConsumable}: its look,
 * animation and flight come from our gun pack's index file named like this item
 * (data/decade/index/throwable/NAME.json), whose "base_item" is this item. lrtactical finds everything through the
 * throwable id in a stack's NBT; here that id is the item's own name, written into every stack so they merge.
 */
public class PackThrowable extends ThrowableItem {
    @Override
    public ResourceLocation getId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ID_TAG, Tag.TAG_STRING)) {
            return super.getId(stack);
        }
        return ownId();
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        setId(stack, ownId());
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        CompoundTag tag = stack.getTag();
        if (!level.isClientSide() && (tag == null || !tag.contains(ID_TAG, Tag.TAG_STRING))) {
            setId(stack, ownId());
        }
    }

    private ResourceLocation ownId() {
        return ForgeRegistries.ITEMS.getKey(this);
    }
}
