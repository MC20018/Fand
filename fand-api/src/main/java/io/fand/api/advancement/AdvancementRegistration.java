package io.fand.api.advancement;

public interface AdvancementRegistration extends AutoCloseable {
    @Override
    void close();
}
