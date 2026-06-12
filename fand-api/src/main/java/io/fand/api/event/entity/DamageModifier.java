package io.fand.api.event.entity;

/**
 * Damage calculation buckets exposed by {@link EntityDamageEvent}.
 */
public enum DamageModifier {
    BASE,
    ARMOR,
    RESISTANCE,
    ABSORPTION,
    ENCHANTMENTS,
    BLOCKING,
    FREEZING,
    HARD_HAT,
    CUSTOM
}
