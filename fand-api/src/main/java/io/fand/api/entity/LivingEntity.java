package io.fand.api.entity;

import io.fand.api.item.ItemStack;
import io.fand.api.item.component.EffectKey;
import io.fand.api.item.component.ItemEquipmentSlot;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * An entity with health (mobs, players, armor stands, etc.).
 *
 * <p>{@link #setHealth(double)} runs on the server thread; off-thread writes are
 * silently rescheduled. Setting health to {@code 0} or below kills the entity
 * via the same path as {@code minecraft:generic_kill} damage, firing the usual
 * death events.
 */
public interface LivingEntity extends Entity {

    /** Current health. {@code 0} means the entity is dead or about to die. */
    double health();

    /** Maximum health, including modifiers from attributes and effects. */
    double maxHealth();

    /** Sets the entity's current health, clamped to {@code [0, maxHealth()]}. */
    void setHealth(double health);

    /** Whether the entity is dead or in its death sequence. */
    boolean dead();

    /** Damages this entity using vanilla generic damage. Marshals to the server thread. */
    void damage(double amount);

    /** Damages this entity, attributing the damage to another entity when supported. */
    void damage(double amount, Entity source);

    /** Heals this entity by {@code amount}. Marshals to the server thread. */
    void heal(double amount);

    /** Current absorption hearts. */
    double absorption();

    /** Sets absorption hearts, clamped by vanilla. Marshals to the server thread. */
    void setAbsorption(double absorption);

    /** Current armor value after equipment and modifiers. */
    int armor();

    /** Attribute instance for {@code key}, if this entity type exposes it. */
    Optional<? extends Attribute> attribute(Key key);

    /** Convenience overload for generated vanilla attribute keys. */
    default Optional<? extends Attribute> attribute(AttributeKey key) {
        return attribute(key.key());
    }

    /** Snapshot of active mob effects. */
    Collection<EntityEffect> effects();

    /** Active effect for {@code key}, if present. */
    Optional<EntityEffect> effect(Key key);

    /** Convenience overload for generated vanilla effect keys. */
    default Optional<EntityEffect> effect(EffectKey key) {
        return effect(key.key());
    }

    /** Adds or updates a mob effect. Marshals to the server thread. */
    void addEffect(EntityEffect effect);

    /** Removes an active mob effect. Marshals to the server thread. */
    void removeEffect(Key key);

    /** Convenience overload for generated vanilla effect keys. */
    default void removeEffect(EffectKey key) {
        removeEffect(key.key());
    }

    /** Item currently equipped in {@code slot}, or {@link ItemStack#EMPTY}. */
    ItemStack equipment(ItemEquipmentSlot slot);

    /** Sets the item equipped in {@code slot}. Empty stacks clear the slot. Marshals to the server thread. */
    void setEquipment(ItemEquipmentSlot slot, ItemStack item);

    default int remainingAir() {
        throw new UnsupportedOperationException("Air supply is not supported");
    }

    default void setRemainingAir(int ticks) {
        throw new UnsupportedOperationException("Air supply is not supported");
    }

    default int maximumAir() {
        throw new UnsupportedOperationException("Maximum air supply is not supported");
    }

    default int freezeTicks() {
        throw new UnsupportedOperationException("Freeze ticks are not supported");
    }

    default void setFreezeTicks(int ticks) {
        throw new UnsupportedOperationException("Freeze ticks are not supported");
    }

    default int invulnerableTicks() {
        throw new UnsupportedOperationException("Invulnerable ticks are not supported");
    }

    default void setInvulnerableTicks(int ticks) {
        throw new UnsupportedOperationException("Invulnerable ticks are not supported");
    }

    default boolean lineOfSight(Entity target) {
        java.util.Objects.requireNonNull(target, "target");
        return false;
    }
}
