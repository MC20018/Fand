package io.fand.api.lifecycle;

import io.fand.api.event.Event;
import io.fand.api.plugin.PluginDescriptor;
import java.util.Objects;

public record PluginDisableEvent(PluginDescriptor plugin) implements Event {
    public PluginDisableEvent {
        Objects.requireNonNull(plugin, "plugin");
    }
}
