package io.fand.api.lifecycle;

import io.fand.api.event.Event;
import io.fand.api.plugin.PluginDescriptor;
import java.util.Objects;

public record PluginEnableEvent(PluginDescriptor plugin) implements Event {
    public PluginEnableEvent {
        Objects.requireNonNull(plugin, "plugin");
    }
}
