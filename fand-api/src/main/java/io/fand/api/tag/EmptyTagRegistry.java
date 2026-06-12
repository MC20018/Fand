package io.fand.api.tag;

import java.util.Collection;
import java.util.Set;
import net.kyori.adventure.key.Key;

enum EmptyTagRegistry implements TagRegistry {
    INSTANCE;

    @Override
    public Set<Key> tagsContaining(TagRegistryType registry, Key value) {
        return Set.of();
    }

    @Override
    public Set<Key> values(TagRegistryType registry, Key tag) {
        return Set.of();
    }

    @Override
    public Collection<Key> tags(TagRegistryType registry) {
        return Set.of();
    }
}
