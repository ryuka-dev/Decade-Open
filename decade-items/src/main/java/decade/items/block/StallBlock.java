package decade.items.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A stall in Decade City: the front middle of a 3×2 booth, drawing the whole booth from the
 * parts its {@link StallBlockEntity} names. The other five cells are {@link StallFillerBlock}s, placed and removed with
 * it. A shell like {@link FixtureBlock}: right-clicking it, here or on any filler, is decade_economy's, by registry name.
 *
 * <p>{@link #LIT} is the lamp; the server turns it on at night, and the block gives light only while it is on.
 */
public class StallBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public StallBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** Faces the one placing it, as a counter does; refused where any of the six cells is taken. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        for (int[] cell : StallFootprint.fillers()) {
            BlockPos at = pos.offset(StallFootprint.offset(facing, cell[0], cell[1]));
            if (!context.getLevel().getBlockState(at).canBeReplaced(context)
                    || !context.getLevel().getWorldBorder().isWithinBounds(at)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    /**
     * The fillers go in whenever the stall block does, by hand or by {@code /setblock} (the world recipe places stalls by
     * command). A cell that is not free is left alone: the stall then simply has no wall there.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level.isClientSide || state.is(oldState.getBlock())) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (int[] cell : StallFootprint.fillers()) {
            BlockPos at = pos.offset(StallFootprint.offset(facing, cell[0], cell[1]));
            if (level.getBlockState(at).canBeReplaced()) {
                level.setBlock(at, StallFillerBlock.state(facing, cell[0], cell[1]), Block.UPDATE_ALL);
            }
        }
    }

    /** Taking the stall away takes its fillers with it. */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            Direction facing = state.getValue(FACING);
            for (int[] cell : StallFootprint.fillers()) {
                BlockPos at = pos.offset(StallFootprint.offset(facing, cell[0], cell[1]));
                if (level.getBlockState(at).getBlock() instanceof StallFillerBlock) {
                    level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Claims the click on both sides, for the reason {@link FixtureBlock#use} gives. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StallBlockEntity(pos, state);
    }
}
