package io.fand.api.player;

import io.fand.api.inventory.PlayerInventory;
import java.time.Instant;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Offline player data loaded from server storage when available.
 */
public interface OfflinePlayer {

    PlayerProfile profile();

    Optional<Instant> firstPlayed();

    Optional<Instant> lastPlayed();

    boolean playedBefore();

    int statistic(Key key);

    Optional<PlayerInventory> inventory();
}
