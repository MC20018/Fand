package io.fand.api.event.entity;

import io.fand.api.entity.AreaEffectCloud;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.List;
import java.util.Objects;

public final class AreaEffectCloudApplyEvent implements Event, Cancellable {
    private final AreaEffectCloud cloud;
    private final List<LivingEntity> affectedEntities;
    private boolean cancelled;

    public AreaEffectCloudApplyEvent(AreaEffectCloud cloud, List<LivingEntity> affectedEntities) {
        this.cloud = Objects.requireNonNull(cloud, "cloud");
        this.affectedEntities = new java.util.ArrayList<>(Objects.requireNonNull(affectedEntities, "affectedEntities"));
    }

    public AreaEffectCloud cloud() {
        return cloud;
    }

    public List<LivingEntity> affectedEntities() {
        return affectedEntities;
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
