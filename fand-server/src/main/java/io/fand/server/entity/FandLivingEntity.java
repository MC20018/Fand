package io.fand.server.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.EntityEffect;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemEquipmentSlot;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.WorldRegistry;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Thin handle around a vanilla {@link net.minecraft.world.entity.LivingEntity}
 * for use by API consumers (mostly event dispatch). Reads follow the public
 * entity contract; mutating writes marshal to the server thread via the
 * underlying server.
 *
 * <p>For {@code ServerPlayer} victims prefer the {@link FandPlayer} cached in
 * the registry — its handle is refreshed across respawns and wires up the
 * inventory/permission services.
 */
public class FandLivingEntity extends FandEntity implements LivingEntity {

    public FandLivingEntity(net.minecraft.world.entity.LivingEntity handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.LivingEntity handle() {
        return (net.minecraft.world.entity.LivingEntity) handle;
    }

    @Override
    public double health() {
        return handle().getHealth();
    }

    @Override
    public double maxHealth() {
        return handle().getMaxHealth();
    }

    @Override
    public void setHealth(double health) {
        var handle = handle();
        runOnServerThread(() -> {
            float clamped = (float) Math.max(0.0, Math.min(health, handle.getMaxHealth()));
            handle.setHealth(clamped);
        });
    }

    @Override
    public boolean dead() {
        return handle().isDeadOrDying();
    }

    @Override
    public void damage(double amount) {
        runOnServerThread(() -> {
            if (handle().level() instanceof net.minecraft.server.level.ServerLevel level) {
                handle().hurtServer(level, handle().damageSources().generic(), (float) Math.max(0.0, amount));
            }
        });
    }

    @Override
    public void damage(double amount, io.fand.api.entity.Entity source) {
        java.util.Objects.requireNonNull(source, "source");
        runOnServerThread(() -> {
            if (!(handle().level() instanceof net.minecraft.server.level.ServerLevel level)) {
                return;
            }
            var sourceHandle = EntityHandles.unwrap(source);
            var damageSource = sourceHandle instanceof net.minecraft.world.entity.LivingEntity living
                    ? handle().damageSources().mobAttack(living)
                    : handle().damageSources().generic();
            handle().hurtServer(level, damageSource, (float) Math.max(0.0, amount));
        });
    }

    @Override
    public void heal(double amount) {
        if (amount <= 0.0) {
            return;
        }
        runOnServerThread(() -> handle().heal((float) amount));
    }

    @Override
    public double absorption() {
        return handle().getAbsorptionAmount();
    }

    @Override
    public void setAbsorption(double absorption) {
        runOnServerThread(() -> handle().setAbsorptionAmount((float) Math.max(0.0, absorption)));
    }

    @Override
    public int armor() {
        return handle().getArmorValue();
    }

    @Override
    public Optional<? extends io.fand.api.entity.Attribute> attribute(Key key) {
        java.util.Objects.requireNonNull(key, "key");
        return EntityAttributes.holder(key)
                .map(handle()::getAttribute)
                .map(attribute -> new FandAttribute(attribute, this::runOnServerThread));
    }

    @Override
    public Collection<EntityEffect> effects() {
        return handle().getActiveEffects().stream()
                .map(EntityEffects::toApi)
                .toList();
    }

    @Override
    public Optional<EntityEffect> effect(Key key) {
        java.util.Objects.requireNonNull(key, "key");
        return EntityEffects.holder(key)
                .map(handle()::getEffect)
                .map(EntityEffects::toApi);
    }

    @Override
    public void addEffect(EntityEffect effect) {
        java.util.Objects.requireNonNull(effect, "effect");
        runOnServerThread(() -> handle().addEffect(EntityEffects.toVanilla(effect)));
    }

    @Override
    public void removeEffect(Key key) {
        java.util.Objects.requireNonNull(key, "key");
        EntityEffects.holder(key).ifPresent(holder -> runOnServerThread(() -> handle().removeEffect(holder)));
    }

    @Override
    public ItemStack equipment(ItemEquipmentSlot slot) {
        java.util.Objects.requireNonNull(slot, "slot");
        return FandItemStacks.fromVanilla(handle().getItemBySlot(EquipmentSlots.toVanilla(slot)));
    }

    @Override
    public void setEquipment(ItemEquipmentSlot slot, ItemStack item) {
        java.util.Objects.requireNonNull(slot, "slot");
        java.util.Objects.requireNonNull(item, "item");
        runOnServerThread(() -> handle().setItemSlot(
                EquipmentSlots.toVanilla(slot),
                FandItemStacks.toVanilla(item)));
    }

    @Override
    public int remainingAir() {
        return handle().getAirSupply();
    }

    @Override
    public void setRemainingAir(int ticks) {
        runOnServerThread(() -> handle().setAirSupply(Math.max(0, ticks)));
    }

    @Override
    public int maximumAir() {
        return handle().getMaxAirSupply();
    }

    @Override
    public int freezeTicks() {
        return handle().getTicksFrozen();
    }

    @Override
    public void setFreezeTicks(int ticks) {
        runOnServerThread(() -> handle().setTicksFrozen(Math.max(0, ticks)));
    }

    @Override
    public int invulnerableTicks() {
        return handle().invulnerableTime;
    }

    @Override
    public void setInvulnerableTicks(int ticks) {
        runOnServerThread(() -> handle().invulnerableTime = Math.max(0, ticks));
    }

    @Override
    public boolean lineOfSight(io.fand.api.entity.Entity target) {
        java.util.Objects.requireNonNull(target, "target");
        return handle().hasLineOfSight(EntityHandles.unwrap(target));
    }
}
