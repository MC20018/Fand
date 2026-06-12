package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

public final class PlayerToggleFlightEvent implements Event, Cancellable {
    private final Player player;
    private final boolean flying;
    private boolean cancelled;

    public PlayerToggleFlightEvent(Player player, boolean flying) {
        this.player = Objects.requireNonNull(player, "player");
        this.flying = flying;
    }

    public Player player() {
        return player;
    }

    public boolean flying() {
        return flying;
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
