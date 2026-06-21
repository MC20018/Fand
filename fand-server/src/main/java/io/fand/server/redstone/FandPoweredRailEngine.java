package io.fand.server.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

public final class FandPoweredRailEngine {
    public void updateState(PoweredRailBlock rail, BlockState state, Level level, BlockPos pos) {
        boolean isPowered = state.getValue(PoweredRailBlock.POWERED);
        boolean shouldPower = level.hasNeighborSignal(pos)
                || this.findPoweredRailSignal(rail, level, pos, state, true, 0)
                || this.findPoweredRailSignal(rail, level, pos, state, false, 0);
        if (shouldPower != isPowered) {
            level.setBlock(pos, state.setValue(PoweredRailBlock.POWERED, shouldPower), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos.below(), rail);
            if (state.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT).isSlope()) {
                level.updateNeighborsAt(pos.above(), rail);
            }
        }
    }

    private boolean findPoweredRailSignal(
            PoweredRailBlock rail,
            Level level,
            BlockPos pos,
            BlockState state,
            boolean forward,
            int searchDepth) {
        if (searchDepth >= 8) {
            return false;
        }

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean checkBelow = true;
        RailShape shape = state.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT);
        switch (shape) {
            case NORTH_SOUTH:
                if (forward) {
                    z++;
                } else {
                    z--;
                }
                break;
            case EAST_WEST:
                if (forward) {
                    x--;
                } else {
                    x++;
                }
                break;
            case ASCENDING_EAST:
                if (forward) {
                    x--;
                } else {
                    x++;
                    y++;
                    checkBelow = false;
                }

                shape = RailShape.EAST_WEST;
                break;
            case ASCENDING_WEST:
                if (forward) {
                    x--;
                    y++;
                    checkBelow = false;
                } else {
                    x++;
                }

                shape = RailShape.EAST_WEST;
                break;
            case ASCENDING_NORTH:
                if (forward) {
                    z++;
                } else {
                    z--;
                    y++;
                    checkBelow = false;
                }

                shape = RailShape.NORTH_SOUTH;
                break;
            case ASCENDING_SOUTH:
                if (forward) {
                    z++;
                    y++;
                    checkBelow = false;
                } else {
                    z--;
                }

                shape = RailShape.NORTH_SOUTH;
        }

        return this.isSameRailWithPower(rail, level, new BlockPos(x, y, z), forward, searchDepth, shape)
                || checkBelow && this.isSameRailWithPower(rail, level, new BlockPos(x, y - 1, z), forward, searchDepth, shape);
    }

    private boolean isSameRailWithPower(
            PoweredRailBlock rail,
            Level level,
            BlockPos pos,
            boolean forward,
            int searchDepth,
            RailShape dir) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(rail)) {
            return false;
        }

        RailShape myShape = state.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT);
        return (dir != RailShape.EAST_WEST || myShape != RailShape.NORTH_SOUTH && myShape != RailShape.ASCENDING_NORTH && myShape != RailShape.ASCENDING_SOUTH)
                && (dir != RailShape.NORTH_SOUTH || myShape != RailShape.EAST_WEST && myShape != RailShape.ASCENDING_EAST && myShape != RailShape.ASCENDING_WEST)
                && state.getValue(PoweredRailBlock.POWERED)
                && (level.hasNeighborSignal(pos) || this.findPoweredRailSignal(rail, level, pos, state, forward, searchDepth + 1));
    }
}
