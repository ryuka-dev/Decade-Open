package decade.items.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A tuned module: one item for every kind and tier. Which kind and tier it is is written onto it as a display tag
 * ({@link #TAG}) when it is tuned, so its name reads "模块：爆头 Ⅱ" anywhere; the numbers it carries are not on the
 * item, the server keeps them under the item's registration number and the tooltip asks for them. A module without the
 * tag (a creative copy) is a plain "模块".
 */
public class ModuleItem extends Item {
    /** {@code {kind: "headshot", tier: 2}}; written by the server, for the name only. */
    public static final String TAG = "decade_module";
    private static final String[] TIERS = {"Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ"};

    public ModuleItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, Tag.TAG_COMPOUND)) {
            return super.getName(stack);
        }
        CompoundTag m = tag.getCompound(TAG);
        String kind = m.getString("kind");
        int tier = m.getInt("tier");
        if (m.getBoolean("trait") && kind.matches("[a-z_]{1,32}")) {
            return Component.translatable("item.decade.module.trait", Component.translatable("module.decade." + kind));
        }
        if (kind.isEmpty() || !kind.matches("[a-z_]{1,32}") || tier < 1 || tier > TIERS.length) {
            return super.getName(stack);
        }
        return Component.translatable("item.decade.module.named",
                Component.translatable("module.decade." + kind), TIERS[tier - 1]);
    }

    /** Writes the display tag; the server calls it when it tunes or merges a module. A trait module stays one. */
    public static void mark(ItemStack stack, String kind, int tier) {
        CompoundTag m = new CompoundTag();
        m.putString("kind", kind);
        m.putInt("tier", tier);
        CompoundTag old = stack.getOrCreateTag().getCompound(TAG);
        if (old.getBoolean("trait")) {
            m.putBoolean("trait", true);
        }
        stack.getOrCreateTag().put(TAG, m);
    }
}
