package io.fand.api.messaging;

public interface PluginMessageRegistration extends AutoCloseable {
    @Override
    void close();
}
