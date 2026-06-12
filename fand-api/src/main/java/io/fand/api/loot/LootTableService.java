package io.fand.api.loot;

import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Public loot-table lookup and generation entry point.
 */
public interface LootTableService {

    Optional<LootTableView> table(Key key);

    default java.util.List<io.fand.api.item.ItemStack> generate(Key key, LootContext context) {
        return java.util.List.of();
    }

    static LootTableService empty() {
        return key -> Optional.empty();
    }
}
