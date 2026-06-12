package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before vanilla applies damage to a
 * {@link LivingEntity}. Cancelling the event aborts the damage application:
 * the victim keeps their current health, no knockback is dealt, and damage
 * immunity timers are not advanced.
 *
 * <p>The damage amount is mutable and reflects the post-armor, post-effect
 * value vanilla intends to apply. Setting it to zero is equivalent to
 * cancelling. Negative values are clamped to zero by the runtime.
 */
public class EntityDamageEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final DamageCause cause;
    private final @Nullable LivingEntity directEntity;
    private final @Nullable LivingEntity attacker;
    private final EnumMap<DamageModifier, Double> modifiers;
    private double amount;
    private boolean cancelled;

    public EntityDamageEvent(LivingEntity entity, String cause, double amount) {
        this(entity, DamageCause.of(cause), amount, null, null);
    }

    public EntityDamageEvent(LivingEntity entity, DamageCause cause, double amount) {
        this(entity, cause, amount, null, null);
    }

    public EntityDamageEvent(
            LivingEntity entity,
            String cause,
            double amount,
            @Nullable LivingEntity directEntity,
            @Nullable LivingEntity attacker) {
        this(entity, DamageCause.of(cause), amount, directEntity, attacker);
    }

    public EntityDamageEvent(
            LivingEntity entity,
            DamageCause cause,
            double amount,
            @Nullable LivingEntity directEntity,
            @Nullable LivingEntity attacker) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.directEntity = directEntity;
        this.attacker = attacker;
        this.amount = amount;
        this.modifiers = new EnumMap<>(DamageModifier.class);
        this.modifiers.put(DamageModifier.BASE, amount);
    }

    public LivingEntity entity() {
        return entity;
    }

    /** Vanilla damage-type identifier (e.g. {@code minecraft:fall}, {@code minecraft:player_attack}). */
    public String cause() {
        return cause.asString();
    }

    /** Typed vanilla damage cause. */
    public DamageCause damageCause() {
        return cause;
    }

    /** Typed vanilla damage type key. */
    public Key causeKey() {
        return cause.key();
    }

    /**
     * Direct living entity involved in the damage, if any. For melee attacks
     * this is usually the attacker; for projectiles it is empty because the
     * direct source is an arrow/fireball rather than a living entity.
     */
    public Optional<LivingEntity> directEntity() {
        return Optional.ofNullable(directEntity);
    }

    /** Living entity credited as causing the damage, if vanilla exposes one. */
    public Optional<LivingEntity> attacker() {
        return Optional.ofNullable(attacker);
    }

    public double amount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
        this.modifiers.clear();
        this.modifiers.put(DamageModifier.BASE, amount);
    }

    public Map<DamageModifier, Double> modifiers() {
        return Map.copyOf(modifiers);
    }

    public double modifier(DamageModifier modifier) {
        return modifiers.getOrDefault(Objects.requireNonNull(modifier, "modifier"), 0.0);
    }

    public void setModifier(DamageModifier modifier, double amount) {
        modifiers.put(Objects.requireNonNull(modifier, "modifier"), amount);
        this.amount = modifiers.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
