package io.fand.server.redstone;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class FandDetectorRailEngine {
    public void checkPressed(DetectorRailBlock rail, Level level, BlockPos pos, BlockState state) {
        if (rail.fand$canSurvive(state, level, pos)) {
            boolean wasPressed = state.getValue(DetectorRailBlock.POWERED);
            boolean shouldBePressed = !this.getInteractingMinecartOfType(level, pos, AbstractMinecart.class, entity -> true).isEmpty();

            if (shouldBePressed && !wasPressed) {
                BlockState newState = state.setValue(DetectorRailBlock.POWERED, true);
                level.setBlock(pos, newState, Block.UPDATE_ALL);
                this.updatePowerToConnected(level, pos, newState);
                level.updateNeighborsAt(pos, rail);
                level.updateNeighborsAt(pos.below(), rail);
                level.setBlocksDirty(pos, state, newState);
            }

            if (!shouldBePressed && wasPressed) {
                BlockState newState = state.setValue(DetectorRailBlock.POWERED, false);
                level.setBlock(pos, newState, Block.UPDATE_ALL);
                this.updatePowerToConnected(level, pos, newState);
                level.updateNeighborsAt(pos, rail);
                level.updateNeighborsAt(pos.below(), rail);
                level.setBlocksDirty(pos, state, newState);
            }

            if (shouldBePressed) {
                level.scheduleTick(pos, rail, 20);
            }

            level.updateNeighbourForOutputSignal(pos, rail);
        }
    }

    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(DetectorRailBlock.POWERED)) {
            List<MinecartCommandBlock> commandBlocks = this.getInteractingMinecartOfType(level, pos, MinecartCommandBlock.class, entity -> true);
            if (!commandBlocks.isEmpty()) {
                return commandBlocks.get(0).getCommandBlock().getSuccessCount();
            }

            List<AbstractMinecart> entities = this.getInteractingMinecartOfType(level, pos, AbstractMinecart.class, EntitySelector.CONTAINER_ENTITY_SELECTOR);
            if (!entities.isEmpty()) {
                return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)entities.get(0));
            }
        }

        return 0;
    }

    private void updatePowerToConnected(Level level, BlockPos pos, BlockState state) {
        RailState rail = new RailState(level, pos, state);

        for (BlockPos connectionPos : rail.getConnections()) {
            BlockState connectionState = level.getBlockState(connectionPos);
            level.neighborChanged(connectionState, connectionPos, connectionState.getBlock(), null, false);
        }
    }

    private <T extends AbstractMinecart> List<T> getInteractingMinecartOfType(
            Level level,
            BlockPos pos,
            Class<T> type,
            Predicate<Entity> containerEntitySelector) {
        return level.getEntitiesOfClass(type, this.getSearchBB(pos), containerEntitySelector);
    }

    private AABB getSearchBB(BlockPos pos) {
        double b = 0.2;
        return new AABB(pos.getX() + b, pos.getY(), pos.getZ() + b, pos.getX() + 1 - b, pos.getY() + 1 - b, pos.getZ() + 1 - b);
    }
}
