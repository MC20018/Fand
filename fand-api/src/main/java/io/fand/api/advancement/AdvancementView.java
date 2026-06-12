package io.fand.api.advancement;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

public record AdvancementView(Key key, @Nullable Component title, @Nullable Component description) {
}
