package io.fand.api.messaging;

import io.fand.api.entity.Player;
import java.util.Collection;
import net.kyori.adventure.key.Key;

/**
 * Standard plugin messaging channel API for custom payloads.
 */
public interface PluginMessaging {

    Collection<PluginMessageChannel> channels();

    default PluginMessageRegistration register(Key channel, PluginMessageDirection direction) {
        throw new UnsupportedOperationException("Plugin messaging channels are not supported");
    }

    default PluginMessageRegistration register(Key channel, PluginMessageDirection direction, PluginMessageHandler handler) {
        throw new UnsupportedOperationException("Plugin messaging channels are not supported");
    }

    default void send(Player player, Key channel, byte[] payload) {
        throw new UnsupportedOperationException("Plugin messaging channels are not supported");
    }

    static PluginMessaging empty() {
        return () -> java.util.List.of();
    }
}
