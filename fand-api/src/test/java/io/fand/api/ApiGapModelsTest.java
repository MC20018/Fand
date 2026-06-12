package io.fand.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.command.CommandArgument;
import io.fand.api.command.CommandArgumentType;
import io.fand.api.event.entity.DamageCause;
import io.fand.api.event.entity.DamageModifier;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.persistence.PersistentDataContainer;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ApiGapModelsTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void persistentDataRoundTripsThroughItemCustomData() {
        var key = Key.key("example", "owner");
        var stack = new ItemStack(DIAMOND, 1)
                .withPersistentData(PersistentDataContainer.EMPTY.withString(key, "alice"));

        assertThat(stack.persistentData().getString(key)).contains("alice");

        var cleared = stack.withoutPersistentData(key);
        assertThat(cleared.persistentData().empty()).isTrue();
        assertThat(cleared.customData()).isEmpty();
    }

    @Test
    void commandArgumentDescribesTypedSelectors() {
        var argument = CommandArgument.players("targets").asOptional();

        assertThat(argument.name()).isEqualTo("targets");
        assertThat(argument.type()).isEqualTo(CommandArgumentType.PLAYERS);
        assertThat(argument.optional()).isTrue();
    }

    @Test
    void damageEventKeepsLegacyCauseAndTypedCause() {
        var event = new EntityDamageEvent(new TestLivingEntity(), DamageCause.FALL, 7.0);

        assertThat(event.cause()).isEqualTo("minecraft:fall");
        assertThat(event.causeKey()).isEqualTo(Key.key("minecraft:fall"));

        event.setModifier(DamageModifier.ARMOR, -2.0);
        assertThat(event.amount()).isEqualTo(5.0);
        assertThat(event.modifiers()).containsEntry(DamageModifier.BASE, 7.0);

        event.setAmount(3.0);
        assertThat(event.amount()).isEqualTo(3.0);
        assertThat(event.modifiers()).containsOnly(Map.entry(DamageModifier.BASE, 3.0));
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private static final class TestLivingEntity implements io.fand.api.entity.LivingEntity {
        @Override
        public java.util.UUID uniqueId() {
            return new java.util.UUID(0L, 1L);
        }

        @Override
        public int entityId() {
            return 1;
        }

        @Override
        public io.fand.api.entity.EntityType type() {
            return new io.fand.api.entity.EntityType() {
                @Override
                public Key key() {
                    return Key.key("minecraft:pig");
                }

                @Override
                public boolean spawnable() {
                    return true;
                }

                @Override
                public boolean player() {
                    return false;
                }
            };
        }

        @Override
        public boolean alive() {
            return true;
        }

        @Override
        public io.fand.api.world.Location location() {
            return null;
        }

        @Override
        public io.fand.api.world.World world() {
            return null;
        }

        @Override
        public io.fand.api.world.Vector3 velocity() {
            return new io.fand.api.world.Vector3(0, 0, 0);
        }

        @Override
        public void setVelocity(io.fand.api.world.Vector3 velocity) {
        }

        @Override
        public java.util.Optional<net.kyori.adventure.text.Component> customName() {
            return java.util.Optional.empty();
        }

        @Override
        public void setCustomName(net.kyori.adventure.text.Component name) {
        }

        @Override
        public boolean customNameVisible() {
            return false;
        }

        @Override
        public void setCustomNameVisible(boolean visible) {
        }

        @Override
        public boolean glowing() {
            return false;
        }

        @Override
        public void setGlowing(boolean glowing) {
        }

        @Override
        public boolean silent() {
            return false;
        }

        @Override
        public void setSilent(boolean silent) {
        }

        @Override
        public boolean gravity() {
            return true;
        }

        @Override
        public void setGravity(boolean gravity) {
        }

        @Override
        public boolean invulnerable() {
            return false;
        }

        @Override
        public void setInvulnerable(boolean invulnerable) {
        }

        @Override
        public java.util.Set<String> scoreboardTags() {
            return java.util.Set.of();
        }

        @Override
        public void addScoreboardTag(String tag) {
        }

        @Override
        public void removeScoreboardTag(String tag) {
        }

        @Override
        public double width() {
            return 1;
        }

        @Override
        public double height() {
            return 1;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> teleport(io.fand.api.world.Location destination) {
            return java.util.concurrent.CompletableFuture.completedFuture(true);
        }

        @Override
        public void remove() {
        }

        @Override
        public java.util.Optional<? extends io.fand.api.entity.Entity> vehicle() {
            return java.util.Optional.empty();
        }

        @Override
        public List<? extends io.fand.api.entity.Entity> passengers() {
            return List.of();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> mount(io.fand.api.entity.Entity vehicle) {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> addPassenger(io.fand.api.entity.Entity passenger) {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> removePassenger(io.fand.api.entity.Entity passenger) {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> dismount() {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }

        @Override
        public void ejectPassengers() {
        }

        @Override
        public boolean onGround() {
            return true;
        }

        @Override
        public boolean inWater() {
            return false;
        }

        @Override
        public boolean inLava() {
            return false;
        }

        @Override
        public int fireTicks() {
            return 0;
        }

        @Override
        public void setFireTicks(int ticks) {
        }

        @Override
        public int ticksLived() {
            return 0;
        }

        @Override
        public io.fand.api.component.DataComponentContainer components() {
            return null;
        }

        @Override
        public double health() {
            return 20;
        }

        @Override
        public double maxHealth() {
            return 20;
        }

        @Override
        public void setHealth(double health) {
        }

        @Override
        public boolean dead() {
            return false;
        }

        @Override
        public void damage(double amount) {
        }

        @Override
        public void damage(double amount, io.fand.api.entity.Entity source) {
        }

        @Override
        public void heal(double amount) {
        }

        @Override
        public double absorption() {
            return 0;
        }

        @Override
        public void setAbsorption(double absorption) {
        }

        @Override
        public int armor() {
            return 0;
        }

        @Override
        public java.util.Optional<? extends io.fand.api.entity.Attribute> attribute(Key key) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Collection<io.fand.api.entity.EntityEffect> effects() {
            return List.of();
        }

        @Override
        public java.util.Optional<io.fand.api.entity.EntityEffect> effect(Key key) {
            return java.util.Optional.empty();
        }

        @Override
        public void addEffect(io.fand.api.entity.EntityEffect effect) {
        }

        @Override
        public void removeEffect(Key key) {
        }

        @Override
        public ItemStack equipment(io.fand.api.item.component.ItemEquipmentSlot slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setEquipment(io.fand.api.item.component.ItemEquipmentSlot slot, ItemStack item) {
        }
    }
}
