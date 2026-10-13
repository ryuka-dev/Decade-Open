package decade.items.block;

import decade.items.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One of the five cells of a stall that are not the stall block (see {@link StallFootprint}): invisible, solid, it keeps
 * people from walking through the booth or building in it. It knows its column and row, so it leads back to its stall;
 * breaking it breaks the stall. Server mods read {@link #COLUMN}, {@link #ROW} and {@code facing} by name.
 */
public class StallFillerBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, StallFootprint.COLUMNS - 1);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, StallFootprint.ROWS - 1);

    public StallFillerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(COLUMN, 0).setValue(ROW, 0));
    }

    static BlockState state(Direction facing, int column, int row) {
        return ItemRegistry.STALL_FILLER.get().defaultBlockState().setValue(FACING, facing).setValue(COLUMN, column).setValue(ROW, row);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW);
    }

    public static BlockPos master(BlockState state, BlockPos pos) {
        return StallFootprint.master(pos, state.getValue(FACING), state.getValue(COLUMN), state.getValue(ROW));
    }

    /** A filler without its stall (left over by a broken world edit) removes itself. */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos master = master(state, pos);
            if (level.getBlockState(master).getBlock() instanceof StallBlock) {
                level.setBlock(master, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Middle-click picks the stall itself. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        BlockState master = level.getBlockState(master(state, pos));
        return master.getBlock() instanceof StallBlock ? new ItemStack(master.getBlock()) : ItemStack.EMPTY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
