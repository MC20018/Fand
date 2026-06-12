package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Vector3;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class EntityKnockbackEvent implements Event, Cancellable {
    private final LivingEntity entity;
    private final @Nullable Entity source;
    private Vector3 velocity;
    private boolean cancelled;

    public EntityKnockbackEvent(LivingEntity entity, @Nullable Entity source, Vector3 velocity) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.source = source;
        this.velocity = Objects.requireNonNull(velocity, "velocity");
    }

    public LivingEntity entity() {
        return entity;
    }

    public java.util.Optional<Entity> source() {
        return java.util.Optional.ofNullable(source);
    }

    public Vector3 velocity() {
        return velocity;
    }

    public void setVelocity(Vector3 velocity) {
        this.velocity = Objects.requireNonNull(velocity, "velocity");
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
