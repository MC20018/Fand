package io.fand.server.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class FandRedstoneLampEngine {
    public void onNeighborChanged(RedstoneLampBlock lamp, BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }

        boolean isLit = state.getValue(RedstoneLampBlock.LIT);
        if (isLit != level.hasNeighborSignal(pos)) {
            if (isLit) {
                level.scheduleTick(pos, lamp, 4);
            } else {
                level.setBlock(pos, state.cycle(RedstoneLampBlock.LIT), Block.UPDATE_CLIENTS);
            }
        }
    }

    public void onTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RedstoneLampBlock.LIT) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(RedstoneLampBlock.LIT), Block.UPDATE_CLIENTS);
        }
    }
}
