package io.fand.server.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;

public final class FandPistonEngine {
    public void checkIfExtend(PistonBaseBlock piston, Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(PistonBaseBlock.FACING);
        boolean extend = this.getNeighborSignal(level, pos, direction);
        if (extend && !state.getValue(PistonBaseBlock.EXTENDED)) {
            if (new PistonStructureResolver(level, pos, direction, true).resolve()) {
                level.blockEvent(pos, piston, PistonBaseBlock.TRIGGER_EXTEND, direction.get3DDataValue());
            }
        } else if (!extend && state.getValue(PistonBaseBlock.EXTENDED)) {
            BlockPos pushedPos = pos.relative(direction, 2);
            BlockState pushedState = level.getBlockState(pushedPos);
            int event = PistonBaseBlock.TRIGGER_CONTRACT;
            if (pushedState.is(Blocks.MOVING_PISTON)
                    && pushedState.getValue(MovingPistonBlock.FACING) == direction
                    && level.getBlockEntity(pushedPos) instanceof PistonMovingBlockEntity pistonEntity
                    && pistonEntity.isExtending()
                    && (pistonEntity.getProgress(0.0F) < 0.5F || level.getGameTime() == pistonEntity.getLastTicked() || ((ServerLevel)level).isHandlingTick())) {
                event = PistonBaseBlock.TRIGGER_DROP;
            }

            level.blockEvent(pos, piston, event, direction.get3DDataValue());
        }
    }

    public boolean getNeighborSignal(SignalGetter level, BlockPos pos, Direction pushDirection) {
        for (Direction direction : Direction.values()) {
            if (direction != pushDirection && level.hasSignal(pos.relative(direction), direction)) {
                return true;
            }
        }

        if (level.hasSignal(pos, Direction.DOWN)) {
            return true;
        }

        BlockPos above = pos.above();

        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN && level.hasSignal(above.relative(direction), direction)) {
                return true;
            }
        }

        return false;
    }

    public boolean triggerEvent(PistonBaseBlock piston, BlockState state, Level level, BlockPos pos, int event, int data) {
        Direction direction = state.getValue(PistonBaseBlock.FACING);
        BlockState extendedState = state.setValue(PistonBaseBlock.EXTENDED, true);
        if (!level.isClientSide()) {
            boolean extend = this.getNeighborSignal(level, pos, direction);
            if (extend && (event == PistonBaseBlock.TRIGGER_CONTRACT || event == PistonBaseBlock.TRIGGER_DROP)) {
                level.setBlock(pos, extendedState, Block.UPDATE_CLIENTS);
                return false;
            }

            if (!extend && event == PistonBaseBlock.TRIGGER_EXTEND) {
                return false;
            }
        }

        if (event == PistonBaseBlock.TRIGGER_EXTEND) {
            return this.extend(piston, level, pos, direction, extendedState);
        }
        if (event == PistonBaseBlock.TRIGGER_CONTRACT || event == PistonBaseBlock.TRIGGER_DROP) {
            this.contract(piston, level, pos, direction, event, data);
        }

        return true;
    }

    private boolean extend(PistonBaseBlock piston, Level level, BlockPos pos, Direction direction, BlockState extendedState) {
        if (!piston.fand$moveBlocks(level, pos, direction, true)) {
            return false;
        }

        level.setBlock(pos, extendedState, Block.UPDATE_ALL | Block.UPDATE_MOVE_BY_PISTON);
        level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
        level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(extendedState));
        return true;
    }

    private void contract(PistonBaseBlock piston, Level level, BlockPos pos, Direction direction, int event, int data) {
        if (level.getBlockEntity(pos.relative(direction)) instanceof PistonMovingBlockEntity pistonMovingBlockEntity) {
            pistonMovingBlockEntity.finalTick();
        }

        BlockState movingPistonState = Blocks.MOVING_PISTON
                .defaultBlockState()
                .setValue(MovingPistonBlock.FACING, direction)
                .setValue(MovingPistonBlock.TYPE, piston.fand$isSticky() ? PistonType.STICKY : PistonType.DEFAULT);
        level.setBlock(pos, movingPistonState, Block.UPDATE_NONE | Block.UPDATE_KNOWN_SHAPE);
        level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(
                pos,
                movingPistonState,
                piston.fand$defaultState().setValue(PistonBaseBlock.FACING, Direction.from3DDataValue(data & 7)),
                direction,
                false,
                true));
        level.updateNeighborsAt(pos, movingPistonState.getBlock());
        movingPistonState.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
        if (piston.fand$isSticky()) {
            this.contractSticky(piston, level, pos, direction, event);
        } else {
            level.removeBlock(pos.relative(direction), false);
        }

        level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.15F + 0.6F);
        level.gameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(movingPistonState));
    }

    private void contractSticky(PistonBaseBlock piston, Level level, BlockPos pos, Direction direction, int event) {
        BlockPos twoPos = pos.offset(direction.getStepX() * 2, direction.getStepY() * 2, direction.getStepZ() * 2);
        BlockState movingState = level.getBlockState(twoPos);
        boolean pistonPiece = false;
        if (movingState.is(Blocks.MOVING_PISTON)
                && level.getBlockEntity(twoPos) instanceof PistonMovingBlockEntity entity
                && entity.getDirection() == direction
                && entity.isExtending()) {
            entity.finalTick();
            pistonPiece = true;
        }

        if (!pistonPiece) {
            if (event != PistonBaseBlock.TRIGGER_CONTRACT
                    || movingState.isAir()
                    || !PistonBaseBlock.isPushable(movingState, level, twoPos, direction.getOpposite(), false, direction)
                    || movingState.getPistonPushReaction() != PushReaction.NORMAL
                    && !movingState.is(Blocks.PISTON)
                    && !movingState.is(Blocks.STICKY_PISTON)) {
                level.removeBlock(pos.relative(direction), false);
            } else {
                piston.fand$moveBlocks(level, pos, direction, false);
            }
        }
    }
}
