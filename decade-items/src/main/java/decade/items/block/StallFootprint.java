package decade.items.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The ground a stall stands on: 3 wide, 2 deep. The stall block itself is the front middle
 * cell; the other five hold {@link StallFillerBlock}s, which know their column and row so any of them leads back to it.
 *
 * <p>Column 0..2 runs from the model's −x to +x, row 0 is the front (the side customers stand on), row 1 the back. The
 * model faces north (its front is −z); {@code facing} is where the stall's front looks.
 */
public final class StallFootprint {
    public static final int COLUMNS = 3;
    public static final int ROWS = 2;
    public static final int MASTER_COLUMN = 1;
    public static final int MASTER_ROW = 0;

    private StallFootprint() {
    }

    /** Where cell (column, row) is relative to the stall block. */
    public static BlockPos offset(Direction facing, int column, int row) {
        int mx = column - MASTER_COLUMN;
        int mz = row - MASTER_ROW;
        // model north → world: north keeps (x, z), east turns (x, z) into (−z, x), and so on by quarter turns
        return switch (facing) {
            case EAST -> new BlockPos(-mz, 0, mx);
            case SOUTH -> new BlockPos(-mx, 0, -mz);
            case WEST -> new BlockPos(mz, 0, -mx);
            default -> new BlockPos(mx, 0, mz);
        };
    }

    /** Every cell but the stall block's own: [column, row]. */
    public static List<int[]> fillers() {
        List<int[]> cells = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (column != MASTER_COLUMN || row != MASTER_ROW) {
                    cells.add(new int[] {column, row});
                }
            }
        }
        return cells;
    }

    /** The stall block a filler at {@code pos} belongs to. */
    public static BlockPos master(BlockPos pos, Direction facing, int column, int row) {
        return pos.subtract(offset(facing, column, row));
    }
}
