package io.fand.server.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

final class FandRedstoneWirePowerChange {
    private FandRedstoneWirePowerChange() {
    }

    static int fire(Level level, BlockPos pos, int oldPower, int newPower) {
        if (level instanceof ServerLevel serverLevel) {
            return io.fand.server.event.BlockEvents.fireRedstone(serverLevel, pos, oldPower, newPower);
        }
        return newPower;
    }
}
