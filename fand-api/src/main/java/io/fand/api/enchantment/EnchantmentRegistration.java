package io.fand.api.enchantment;

public interface EnchantmentRegistration extends AutoCloseable {
    @Override
    void close();
}
