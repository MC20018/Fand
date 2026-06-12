package io.fand.api.structure;

import net.kyori.adventure.key.Key;

public record StructureTemplate(Key key, int width, int height, int depth) {
}
