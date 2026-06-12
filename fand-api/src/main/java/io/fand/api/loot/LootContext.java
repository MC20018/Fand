package io.fand.api.loot;

import io.fand.api.entity.Entity;
import io.fand.api.world.Location;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record LootContext(
        @Nullable Location location,
        @Nullable Entity killer,
        float luck
) {
    public Optional<Location> locationOptional() {
        return Optional.ofNullable(location);
    }

    public Optional<Entity> killerOptional() {
        return Optional.ofNullable(killer);
    }

    public static LootContext empty() {
        return new LootContext(null, null, 0.0F);
    }
}
