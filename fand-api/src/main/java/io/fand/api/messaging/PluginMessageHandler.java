package io.fand.api.messaging;

import io.fand.api.entity.Player;

@FunctionalInterface
public interface PluginMessageHandler {
    void handle(Player player, PluginMessageChannel channel, byte[] payload);
}
