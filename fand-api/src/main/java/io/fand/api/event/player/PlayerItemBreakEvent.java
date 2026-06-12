package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

public final class PlayerItemBreakEvent implements Event {
    private final Player player;
    private final ItemStack item;

    public PlayerItemBreakEvent(Player player, ItemStack item) {
        this.player = Objects.requireNonNull(player, "player");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Player player() {
        return player;
    }

    public ItemStack item() {
        return item;
    }
}
