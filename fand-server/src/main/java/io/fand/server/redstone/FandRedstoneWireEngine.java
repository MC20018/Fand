package io.fand.server.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public final class FandRedstoneWireEngine {
    private final RedStoneWireBlock wireBlock;
    private final FandRedstoneWireDefaultPath defaultPath;

    public FandRedstoneWireEngine(RedStoneWireBlock wireBlock) {
        this.wireBlock = wireBlock;
        this.defaultPath = new FandRedstoneWireDefaultPath(wireBlock);
    }

    public void updatePowerStrength(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Orientation orientation,
            boolean shapeUpdateWiresAroundInitialPosition) {
        if (this.usesExperimentalRules(level)) {
            new FandRedstoneWireExperimentalPath(this.wireBlock)
                    .updatePowerStrength(level, pos, state, orientation, shapeUpdateWiresAroundInitialPosition);
        } else {
            this.defaultPath.updatePowerStrength(level, pos, state, orientation, shapeUpdateWiresAroundInitialPosition);
        }
    }

    public boolean skipsSelfNeighborUpdate(Level level, Block sourceBlock) {
        return sourceBlock == this.wireBlock && this.usesExperimentalRules(level);
    }

    private boolean usesExperimentalRules(Level level) {
        return level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS);
    }
}
