package io.fand.api.event.entity;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Typed vanilla damage cause identifier.
 */
public record DamageCause(Key key) {

    public static final DamageCause GENERIC = minecraft("generic");
    public static final DamageCause PLAYER_ATTACK = minecraft("player_attack");
    public static final DamageCause MOB_ATTACK = minecraft("mob_attack");
    public static final DamageCause PROJECTILE = minecraft("arrow");
    public static final DamageCause FALL = minecraft("fall");
    public static final DamageCause FIRE = minecraft("in_fire");
    public static final DamageCause LAVA = minecraft("lava");
    public static final DamageCause DROWN = minecraft("drown");
    public static final DamageCause EXPLOSION = minecraft("explosion");
    public static final DamageCause MAGIC = minecraft("magic");
    public static final DamageCause OUT_OF_WORLD = minecraft("out_of_world");

    public DamageCause {
        Objects.requireNonNull(key, "key");
    }

    public static DamageCause of(Key key) {
        return new DamageCause(key);
    }

    public static DamageCause of(String key) {
        return of(Key.key(key));
    }

    public static DamageCause minecraft(String value) {
        return of(Key.key(Key.MINECRAFT_NAMESPACE, value));
    }

    public String asString() {
        return key.asString();
    }
}
