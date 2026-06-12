package io.fand.api.enchantment;

import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public record CustomEnchantment(Key key, Component description, int maxLevel) {
    public CustomEnchantment {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(description, "description");
        if (maxLevel < 1) {
            throw new IllegalArgumentException("maxLevel must be >= 1");
        }
    }
}
