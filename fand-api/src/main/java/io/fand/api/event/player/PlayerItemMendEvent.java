package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

public final class PlayerItemMendEvent implements Event, Cancellable {
    private final Player player;
    private final ItemStack item;
    private int repairAmount;
    private boolean cancelled;

    public PlayerItemMendEvent(Player player, ItemStack item, int repairAmount) {
        this.player = Objects.requireNonNull(player, "player");
        this.item = Objects.requireNonNull(item, "item");
        this.repairAmount = repairAmount;
    }

    public Player player() {
        return player;
    }

    public ItemStack item() {
        return item;
    }

    public int repairAmount() {
        return repairAmount;
    }

    public void setRepairAmount(int repairAmount) {
        this.repairAmount = Math.max(0, repairAmount);
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
