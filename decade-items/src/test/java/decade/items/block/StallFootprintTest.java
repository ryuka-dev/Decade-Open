package decade.items.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class StallFootprintTest {
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    @Test
    void fiveFillersAroundTheStallBlock() {
        assertEquals(5, StallFootprint.fillers().size());
        for (Direction facing : FACINGS) {
            Set<BlockPos> cells = new HashSet<>();
            cells.add(BlockPos.ZERO);
            for (int[] c : StallFootprint.fillers()) {
                cells.add(StallFootprint.offset(facing, c[0], c[1]));
            }
            assertEquals(6, cells.size(), "cells overlap facing " + facing);
        }
    }

    @Test
    void theBackRowIsBehindTheFront() {
        // facing is where the front looks; the back row is one step the other way
        for (Direction facing : FACINGS) {
            BlockPos back = StallFootprint.offset(facing, StallFootprint.MASTER_COLUMN, 1);
            assertEquals(BlockPos.ZERO.relative(facing.getOpposite()), back, "facing " + facing);
        }
    }

    @Test
    void columnsRunAlongTheFront() {
        for (Direction facing : FACINGS) {
            BlockPos left = StallFootprint.offset(facing, 0, 0);
            BlockPos right = StallFootprint.offset(facing, 2, 0);
            assertEquals(BlockPos.ZERO, left.offset(right), "the two side columns mirror each other, facing " + facing);
            assertTrue(left.getY() == 0 && Math.abs(left.getX()) + Math.abs(left.getZ()) == 1);
            assertEquals(0, left.getX() * facing.getStepX() + left.getZ() * facing.getStepZ(), "sideways, facing " + facing);
        }
    }

    @Test
    void anyFillerLeadsBackToItsStall() {
        BlockPos stall = new BlockPos(379, 72, 327);
        for (Direction facing : FACINGS) {
            for (int[] c : StallFootprint.fillers()) {
                BlockPos filler = stall.offset(StallFootprint.offset(facing, c[0], c[1]));
                assertEquals(stall, StallFootprint.master(filler, facing, c[0], c[1]));
            }
        }
    }
}
