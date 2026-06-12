package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

public final class PlayerAnimationEvent implements Event, Cancellable {
    public enum Animation {
        SWING_MAIN_HAND,
        SWING_OFF_HAND
    }

    private final Player player;
    private final Animation animation;
    private boolean cancelled;

    public PlayerAnimationEvent(Player player, Animation animation) {
        this.player = Objects.requireNonNull(player, "player");
        this.animation = Objects.requireNonNull(animation, "animation");
    }

    public Player player() {
        return player;
    }

    public Animation animation() {
        return animation;
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
