package decade.items.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The ruin survey wand: an admin tool for registering ruins and checking their spawn points. Only
 * the shell lives here. What its clicks do is decided on the server (decade_ruins, by this item's
 * registry name), and only for admins; in anyone else's hand it is a stick.
 */
public class SurveyWand extends Item {
    private static final int TIP_LINES = 5;

    public SurveyWand() {
        super(new Properties().stacksTo(1));
    }

    /** Never breaks blocks, so a left click to pick a corner does not dig, not even in creative. */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    /** Takes the click, so the other hand is not used on the block after it (placing a block, say). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= TIP_LINES; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".tip" + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
