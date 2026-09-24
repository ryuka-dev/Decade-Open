package decade.items.item;

import me.xjqsh.lrtactical.item.ConsumableItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A consumable run by lrtactical but registered as our own item. Its look, animation and use come
 * from our gun pack: the index file named like this item (data/decade/index/consumable/NAME.json),
 * whose "base_item" is this item.
 *
 * <p>lrtactical finds everything through the consumable id in a stack's NBT. Here that id is the
 * item's own name, so a stack without it (from /give, say) still works, and each stack gets it
 * written in so that such stacks merge with the rest.
 */
public class PackConsumable extends ConsumableItem {
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
