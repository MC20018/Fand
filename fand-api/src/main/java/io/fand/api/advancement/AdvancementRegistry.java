package io.fand.api.advancement;

import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Advancement lookup and custom advancement registration.
 */
public interface AdvancementRegistry {

    Optional<AdvancementView> advancement(Key key);

    default AdvancementRegistration register(CustomAdvancement advancement) {
        throw new UnsupportedOperationException("Custom advancement registration is not supported");
    }

    static AdvancementRegistry empty() {
        return key -> Optional.empty();
    }
}
