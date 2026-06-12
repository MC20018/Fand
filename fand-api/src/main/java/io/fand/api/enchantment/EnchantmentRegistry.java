package io.fand.api.enchantment;

import java.util.Optional;
import net.kyori.adventure.key.Key;

public interface EnchantmentRegistry {

    Optional<EnchantmentView> enchantment(Key key);

    default EnchantmentRegistration register(CustomEnchantment enchantment) {
        throw new UnsupportedOperationException("Custom enchantment registration is not supported");
    }

    static EnchantmentRegistry empty() {
        return key -> Optional.empty();
    }
}
