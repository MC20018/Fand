package io.fand.api.advancement;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public record CustomAdvancement(
        Key key,
        Component title,
        Component description,
        List<String> criteria
) {
    public CustomAdvancement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        criteria = List.copyOf(criteria);
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("custom advancement must have at least one criterion");
        }
    }
}
