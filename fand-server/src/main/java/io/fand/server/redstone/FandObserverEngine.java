package io.fand.server.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

public final class FandObserverEngine {
    public void onTick(ObserverBlock observer, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ObserverBlock.POWERED)) {
            level.setBlock(pos, state.setValue(ObserverBlock.POWERED, false), Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, state.setValue(ObserverBlock.POWERED, true), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, observer, 2);
        }

        this.updateNeighborsInFront(observer, level, pos, state);
    }

    public void onShapeUpdate(
            ObserverBlock observer,
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour) {
        if (state.getValue(BlockStateProperties.FACING) == directionToNeighbour && !state.getValue(ObserverBlock.POWERED)) {
            this.startSignal(observer, level, ticks, pos);
        }
    }

    public void startSignal(ObserverBlock observer, LevelReader level, ScheduledTickAccess ticks, BlockPos pos) {
        if (!level.isClientSide() && !ticks.getBlockTicks().hasScheduledTick(pos, observer)) {
            ticks.scheduleTick(pos, observer, 2);
        }
    }

    public void updateNeighborsInFront(ObserverBlock observer, Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(BlockStateProperties.FACING);
        BlockPos oppositePos = pos.relative(direction.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, direction.getOpposite(), null);
        level.neighborChanged(oppositePos, observer, orientation);
        level.updateNeighborsAtExceptFromFacing(oppositePos, observer, direction, orientation);
    }

    public void onPlace(ObserverBlock observer, BlockState state, Level level, BlockPos pos, BlockState oldState) {
        if (!state.is(oldState.getBlock())) {
            if (!level.isClientSide() && state.getValue(ObserverBlock.POWERED) && !level.getBlockTicks().hasScheduledTick(pos, observer)) {
                BlockState newState = state.setValue(ObserverBlock.POWERED, false);
                level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                this.updateNeighborsInFront(observer, level, pos, newState);
            }
        }
    }

    public void afterRemoval(ObserverBlock observer, BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(ObserverBlock.POWERED) && level.getBlockTicks().hasScheduledTick(pos, observer)) {
            this.updateNeighborsInFront(observer, level, pos, state.setValue(ObserverBlock.POWERED, false));
        }
    }
}
