package io.fand.api.enchantment;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

public record EnchantmentView(Key key, @Nullable Component description, int maxLevel) {
}
