package decade.items.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A note found in a ruin: only its title shows, the name the server wrote on it. Used, a server mod takes it into the
 * player's collection in the handbook; the text is the server's and is never on the item.
 */
public class NoteItem extends Item {
    public NoteItem() {
        super(new Properties().stacksTo(16));
    }

    /** The hand swings at once on the client; what using it does is the server mod's, which handles the use first. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.decade.note.hint").withStyle(ChatFormatting.GRAY));
    }
}
