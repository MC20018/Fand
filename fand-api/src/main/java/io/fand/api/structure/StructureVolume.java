package io.fand.api.structure;

import io.fand.api.world.World;

public record StructureVolume(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
}
