package io.fand.api.messaging;

import net.kyori.adventure.key.Key;

public record PluginMessageChannel(Key key, PluginMessageDirection direction) {
}
