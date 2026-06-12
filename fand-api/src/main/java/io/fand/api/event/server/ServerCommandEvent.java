package io.fand.api.event.server;

import io.fand.api.command.CommandSender;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

public final class ServerCommandEvent implements Event, Cancellable {
    private final CommandSender sender;
    private String commandLine;
    private boolean cancelled;

    public ServerCommandEvent(CommandSender sender, String commandLine) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.commandLine = Objects.requireNonNull(commandLine, "commandLine");
    }

    public CommandSender sender() {
        return sender;
    }

    public String commandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = Objects.requireNonNull(commandLine, "commandLine");
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
