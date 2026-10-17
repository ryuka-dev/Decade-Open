package decade.items.block;

import decade.items.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The public salvage bench: it wears Survivor's Arsenal's recycler model, which is two blocks wide, so placing it also
 * places an invisible {@link SalvageBenchHalfBlock} on the side the model reaches into, and removing either removes the
 * other. What using it does is a server mod's, by this block's name.
 */
public class SalvageBenchBlock extends FixtureBlock {

    public SalvageBenchBlock(Properties properties) {
        super(properties);
    }

    /** Where the second half stands: the recycler model reaches one block to the facing's left. */
    public static BlockPos half(BlockPos pos, Direction facing) {
        return pos.relative(facing.getCounterClockWise());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level.isClientSide || state.is(old.getBlock())) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockPos at = half(pos, facing);
        if (level.getBlockState(at).canBeReplaced()) {
            level.setBlock(at, ItemRegistry.SALVAGE_BENCH_HALF.get().defaultBlockState().setValue(FACING, facing),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos at = half(pos, state.getValue(FACING));
            if (level.getBlockState(at).getBlock() instanceof SalvageBenchHalfBlock) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
