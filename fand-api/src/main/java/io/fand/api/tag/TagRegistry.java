package io.fand.api.tag;

import java.util.Collection;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Read-only view of vanilla data-pack tags.
 */
public interface TagRegistry {

    /** Tags in {@code registry} that currently contain {@code value}. */
    Set<Key> tagsContaining(TagRegistryType registry, Key value);

    /** Values currently contained by {@code tag} in {@code registry}. */
    Set<Key> values(TagRegistryType registry, Key tag);

    default boolean contains(TagRegistryType registry, Key tag, Key value) {
        return values(registry, tag).contains(value);
    }

    /** Snapshot of tag keys currently known for {@code registry}. */
    Collection<Key> tags(TagRegistryType registry);

    static TagRegistry empty() {
        return EmptyTagRegistry.INSTANCE;
    }
}
