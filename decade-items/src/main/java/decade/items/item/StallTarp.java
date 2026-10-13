package decade.items.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A tarp found in ruins and airdrops: held to use, with the bow's pose, and once
 * used up it unlocks its pattern for the user's stall roof for good. Letting go early does nothing. Only the shell lives
 * here; what a finished use does is decade_economy's on the server, by this item's registry name
 * ({@code decade:tarp_<pattern>}), which also decides whether one is used up. Its name and icon are in the client jar
 * (tools/stall/build.py), since the patterns are stall art.
 */
public class StallTarp extends Item {
    /** How long it is held, in ticks: as long as eating. */
    private static final int USE_TICKS = 32;

    public StallTarp() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.decade.stall_tarp.use").withStyle(ChatFormatting.GRAY));
    }
}
