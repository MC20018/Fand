package io.fand.api.structure;

public record StructurePlacement(float rotationDegrees, boolean mirror, boolean includeEntities) {
    public static StructurePlacement defaults() {
        return new StructurePlacement(0.0F, false, true);
    }
}
