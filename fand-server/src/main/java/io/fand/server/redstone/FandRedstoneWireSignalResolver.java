package io.fand.server.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;

class FandRedstoneWireSignalResolver {
    private final RedStoneWireBlock wireBlock;

    FandRedstoneWireSignalResolver(RedStoneWireBlock wireBlock) {
        this.wireBlock = wireBlock;
    }

    int calculateTargetStrength(Level level, BlockPos pos) {
        int blockSignal = this.wireBlock.getBlockSignal(level, pos);
        return blockSignal == Redstone.SIGNAL_MAX ? blockSignal : Math.max(blockSignal, this.getIncomingWireSignal(level, pos));
    }

    int getIncomingWireSignal(Level level, BlockPos pos) {
        int wireSignal = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            wireSignal = Math.max(wireSignal, this.getWireSignal(neighborPos, neighborState));
            BlockPos abovePos = pos.above();
            if (neighborState.isRedstoneConductor(level, neighborPos) && !level.getBlockState(abovePos).isRedstoneConductor(level, abovePos)) {
                BlockPos aboveNeighborPos = neighborPos.above();
                wireSignal = Math.max(wireSignal, this.getWireSignal(aboveNeighborPos, level.getBlockState(aboveNeighborPos)));
            } else if (!neighborState.isRedstoneConductor(level, neighborPos)) {
                BlockPos belowNeighborPos = neighborPos.below();
                wireSignal = Math.max(wireSignal, this.getWireSignal(belowNeighborPos, level.getBlockState(belowNeighborPos)));
            }
        }

        return Math.max(0, wireSignal - 1);
    }

    int getWireSignal(BlockPos pos, BlockState state) {
        return state.is(this.wireBlock) ? state.getValue(RedStoneWireBlock.POWER) : 0;
    }
}
