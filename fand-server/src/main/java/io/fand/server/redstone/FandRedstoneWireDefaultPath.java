package io.fand.server.redstone;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

final class FandRedstoneWireDefaultPath {
    private final RedStoneWireBlock wireBlock;
    private final FandRedstoneWireSignalResolver signals;

    FandRedstoneWireDefaultPath(RedStoneWireBlock wireBlock) {
        this.wireBlock = wireBlock;
        this.signals = new FandRedstoneWireSignalResolver(wireBlock);
    }

    void updatePowerStrength(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Orientation orientation,
            boolean shapeUpdateWiresAroundInitialPosition) {
        int targetStrength = this.signals.calculateTargetStrength(level, pos);
        int oldStrength = state.getValue(RedStoneWireBlock.POWER);
        targetStrength = FandRedstoneWirePowerChange.fire(level, pos, oldStrength, targetStrength);
        if (oldStrength != targetStrength) {
            if (level.getBlockState(pos) == state) {
                level.setBlock(pos, state.setValue(RedStoneWireBlock.POWER, targetStrength), Block.UPDATE_CLIENTS);
            }

            Set<BlockPos> toUpdate = Sets.newHashSet();
            toUpdate.add(pos);

            for (Direction direction : Direction.values()) {
                toUpdate.add(pos.relative(direction));
            }

            for (BlockPos blockPos : toUpdate) {
                level.updateNeighborsAt(blockPos, this.wireBlock);
            }
        }
    }
}
