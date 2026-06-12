package io.fand.api.entity;

import io.fand.api.command.CommandSender;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemType;
import io.fand.api.item.ItemStack;
import io.fand.api.permission.PermissionSubject;
import io.fand.api.player.ResourcePackRequest;
import io.fand.api.player.RespawnLocation;
import io.fand.api.player.PlayerProfile;
import io.fand.api.player.PlayerSkin;
import io.fand.api.player.StatisticKey;
import io.fand.api.recipe.Recipe;
import io.fand.api.world.Location;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.sound.SoundEffect;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * A player connected to the server. Instances are thin handles backed by the
 * vanilla player object: equality is by {@link #uniqueId()} and a handle may
 * become {@linkplain #online() offline} after the player disconnects.
 *
 * <p>An offline player handle remains valid as a reference but read methods
 * return their last-known values. {@link #alive()} returns {@code false} once
 * the player disconnects. Player state mutations and packet sends marshal to
 * the server thread unless a method documents a stricter requirement.
 */
public interface Player extends LivingEntity, CommandSender, PermissionSubject {

    /** Whether the player is still connected. */
    boolean online();

    /**
     * The player's connection latency in milliseconds, as a rolling average
     * vanilla keeps from keep-alive round-trips. Returns the last-known value
     * once the player is {@linkplain #online() offline}.
     */
    int ping();

    /**
     * The client-side options this player last reported. Returns the
     * last-known settings once the player is
     * {@linkplain #online() offline}; defaults until the client sends its
     * first settings packet.
     */
    ClientSettings clientSettings();

    /** Current profile identity and texture data used for this player. */
    default PlayerProfile profile() {
        return new PlayerProfile(uniqueId(), name());
    }

    /** Current skin texture, if one is present on the player's profile. */
    default Optional<PlayerSkin> skin() {
        return profile().skin();
    }

    /**
     * Replaces the player's skin texture and refreshes player-info packets for
     * online viewers.
     */
    default void setSkin(@Nullable PlayerSkin skin) {
        throw new UnsupportedOperationException("Player skin changes are not supported");
    }

    /** Resends the player's current profile/skin to online viewers. */
    default void refreshSkin() {
        setSkin(skin().orElse(null));
    }

    /** Disconnects the player with the given reason. No-op if already offline. */
    void kick(Component reason);

    /** Plays a sound at this player's current location. No-op if already offline. */
    void playSound(SoundEffect sound);

    /** Plays a sound at {@code location} for this player. No-op if already offline or in another world. */
    void playSound(Location location, SoundEffect sound);

    default Location eyeLocation() {
        var location = location();
        return new Location(
                location.world(),
                location.x(),
                location.y() + Math.max(0.0, height() * 0.85),
                location.z(),
                location.yaw(),
                location.pitch());
    }

    /** Spawns a single particle at {@code location} for this player. No-op if offline or in another world. */
    default void spawnParticle(Location location, ParticleEffect effect) {
        spawnParticle(location, effect, ParticleEmission.SINGLE);
    }

    /** Spawns particles at {@code location} for this player. No-op if offline or in another world. */
    void spawnParticle(Location location, ParticleEffect effect, ParticleEmission emission);

    /** Sends a tab-list header and footer to this player. */
    void sendTabList(Component header, Component footer);

    /** Clears the tab-list header and footer previously sent to this player. */
    void clearTabList();

    /** Custom tab-list row display name, if one is set. */
    Optional<Component> tabListDisplayName();

    /** Sets or clears this player's tab-list row display name and syncs viewers. */
    void setTabListDisplayName(@Nullable Component displayName);

    /** Sort order for this player's tab-list row. Lower values appear first. */
    int tabListOrder();

    /** Sets this player's tab-list row sort order and syncs viewers. */
    void setTabListOrder(int order);

    /** Pushes a resource-pack request to this player. */
    void sendResourcePack(ResourcePackRequest request);

    /** Removes the resource pack with {@code id}, or all pushed packs when {@code null}. */
    void removeResourcePack(@Nullable UUID id);

    /** Convenience overload that removes every pushed resource pack. */
    default void removeResourcePacks() {
        removeResourcePack(null);
    }

    /**
     * Teleports the player to {@code destination}. Schedules the move on the
     * server thread; the returned future completes with {@code true} on success
     * or {@code false} if the player went offline before the teleport ran.
     */
    CompletableFuture<Boolean> teleport(Location destination);

    /** The player's main inventory + hotbar. */
    io.fand.api.inventory.PlayerInventory inventory();

    /** The player's current game mode. */
    GameMode gameMode();

    /**
     * Switches the player to {@code mode}. Marshals to the server thread when
     * called from elsewhere; takes effect on the next tick at the latest.
     */
    void setGameMode(GameMode mode);

    /** Current food level (0-20). */
    int foodLevel();

    /**
     * Sets the food level, clamped to {@code [0, 20]}. Marshals to the server
     * thread when called from elsewhere.
     */
    void setFoodLevel(int level);

    /** Current saturation. */
    float saturation();

    /**
     * Sets saturation, clamped to {@code [0, foodLevel()]} by vanilla on the
     * next eat. Marshals to the server thread when called from elsewhere.
     */
    void setSaturation(float saturation);

    /** The XP level shown in the action bar. */
    int experienceLevel();

    /** Sets the XP level. Marshals to the server thread. */
    void setExperienceLevel(int level);

    /** Progress toward the next level, in {@code [0, 1)}. */
    float experienceProgress();

    /**
     * Sets the in-bar progress toward the next level. Values are clamped to
     * {@code [0, 1)}; vanilla rejects exact 1.0. Marshals to the server thread.
     */
    void setExperienceProgress(float progress);

    /** Awards {@code points} XP, possibly leveling the player up. */
    void giveExperience(int points);

    /** Whether the player is currently flying. */
    boolean flying();

    /**
     * Toggles flight. Has no effect (and resyncs to the client) when
     * {@link #allowFlight()} is {@code false}. Marshals to the server thread.
     */
    void setFlying(boolean flying);

    /** Whether the player is allowed to fly. */
    boolean allowFlight();

    /**
     * Sets whether flight is permitted. Disabling flight while the player is
     * already flying forces them to drop. Marshals to the server thread.
     */
    void setAllowFlight(boolean allow);

    default float flySpeed() {
        throw new UnsupportedOperationException("Fly speed is not supported");
    }

    default void setFlySpeed(float speed) {
        throw new UnsupportedOperationException("Fly speed is not supported");
    }

    default float walkSpeed() {
        throw new UnsupportedOperationException("Walk speed is not supported");
    }

    default void setWalkSpeed(float speed) {
        throw new UnsupportedOperationException("Walk speed is not supported");
    }

    default void sendBlockChange(Location location, io.fand.api.block.BlockType type) {
        throw new UnsupportedOperationException("Client-side block changes are not supported");
    }

    default void hideEntity(Player viewer, Entity entity) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        java.util.Objects.requireNonNull(entity, "entity");
        throw new UnsupportedOperationException("Per-viewer entity hiding is not supported");
    }

    default void showEntity(Player viewer, Entity entity) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        java.util.Objects.requireNonNull(entity, "entity");
        throw new UnsupportedOperationException("Per-viewer entity hiding is not supported");
    }

    default void openBook(ItemStack book) {
        throw new UnsupportedOperationException("Opening books is not supported");
    }

    default void openSign(Location location) {
        throw new UnsupportedOperationException("Opening signs is not supported");
    }

    default void respawn() {
        throw new UnsupportedOperationException("Forced respawn is not supported");
    }

    /** Personal respawn location, if one is set. */
    Optional<RespawnLocation> respawnLocation();

    /** Sets or clears this player's personal respawn location. */
    void setRespawnLocation(@Nullable RespawnLocation location);

    /** Sends a compass/spawn-target marker to the client without changing server respawn data. */
    void sendCompassTarget(Location location);

    /** Reads a vanilla custom statistic value. */
    int statistic(Key key);

    /** Convenience overload for generated vanilla statistic keys. */
    default int statistic(StatisticKey key) {
        return statistic(key.key());
    }

    /** Sets a vanilla custom statistic value and syncs it to the client. */
    void setStatistic(Key key, int value);

    /** Convenience overload for generated vanilla statistic keys. */
    default void setStatistic(StatisticKey key, int value) {
        setStatistic(key.key(), value);
    }

    /** Adds {@code delta} to a vanilla custom statistic and syncs it to the client. */
    default void incrementStatistic(Key key, int delta) {
        long value = (long) statistic(key) + delta;
        setStatistic(key, (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value)));
    }

    /** Grants one or more recipes to this player's recipe book. */
    int discoverRecipes(Collection<? extends Recipe> recipes);

    /** Removes one or more recipes from this player's recipe book. */
    int undiscoverRecipes(Collection<? extends Recipe> recipes);

    /** Whether {@code type}'s cooldown group is currently cooling down for this player. */
    boolean hasCooldown(ItemType type);

    /** Remaining cooldown fraction for {@code type}'s cooldown group, in {@code [0, 1]}. */
    float cooldownPercent(ItemType type);

    /** Starts or replaces {@code type}'s cooldown group for {@code ticks}; {@code 0} clears it. */
    void setCooldown(ItemType type, int ticks);

    /** Clears {@code type}'s cooldown group. */
    void clearCooldown(ItemType type);

    default boolean hasCooldown(Key group) {
        throw new UnsupportedOperationException("Cooldown groups are not supported");
    }

    default float cooldownPercent(Key group) {
        throw new UnsupportedOperationException("Cooldown groups are not supported");
    }

    default void setCooldown(Key group, int ticks) {
        throw new UnsupportedOperationException("Cooldown groups are not supported");
    }

    default void clearCooldown(Key group) {
        setCooldown(group, 0);
    }

    default Optional<AdvancementProgress> advancementProgress(Key advancement) {
        return Optional.empty();
    }

    default boolean grantAdvancement(Key advancement) {
        return false;
    }

    default boolean revokeAdvancement(Key advancement) {
        return false;
    }

    default boolean grantAdvancementCriterion(Key advancement, String criterion) {
        return false;
    }

    default boolean revokeAdvancementCriterion(Key advancement, String criterion) {
        return false;
    }

    default boolean visibleInPlayerList(Player viewer) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        return true;
    }

    default void setVisibleInPlayerList(Player viewer, boolean visible) {
        java.util.Objects.requireNonNull(viewer, "viewer");
    }

    /** Item currently carried by the cursor in the player's open menu. */
    ItemStack cursorItem();

    /** Replaces the item carried by the cursor in the player's open menu. */
    void setCursorItem(ItemStack item);

    /**
     * Opens a transient container of the given {@code type} for this player,
     * backed by an empty server-side inventory. The future completes on the
     * server thread once the menu is shown.
     *
     * <p>{@code size} is honoured for variable-size types (CHEST: 9-54 in
     * multiples of 9, HOPPER: ignored, etc.) and ignored for fixed-size
     * types. Pass {@code 0} to use the type's default size.
     *
     * <p>Returns {@link Optional#empty()} when the player is offline, the
     * type is {@link InventoryType#PLAYER} or {@link InventoryType#UNKNOWN},
     * the menu type isn't supported by this server, or an
     * {@link io.fand.api.event.inventory.InventoryOpenEvent} listener
     * cancelled the open.
     *
     * @throws IllegalArgumentException if {@code size} is invalid for the
     *         requested {@code type} (e.g. not a multiple of 9 for CHEST)
     */
    CompletableFuture<Optional<Inventory>> openInventory(InventoryType type, int size);

    /**
     * Convenience overload using the type's default size — equivalent to
     * {@code openInventory(type, 0)}.
     */
    default CompletableFuture<Optional<Inventory>> openInventory(InventoryType type) {
        return openInventory(type, 0);
    }

    /**
     * Shows {@code inventory} to this player. The inventory must be one
     * created via {@link io.fand.api.inventory.Inventories#create} — passing
     * a {@link io.fand.api.inventory.PlayerInventory} or an inventory
     * surfaced through events is rejected.
     *
     * <p>The future completes with {@code true} once the menu is shown, or
     * {@code false} when the player is offline, the inventory isn't
     * shareable, or an open listener cancelled.
     */
    CompletableFuture<Boolean> openInventory(Inventory inventory);

    /**
     * The container the player is currently viewing, if any. Returns
     * {@link Optional#empty()} when the player only has their own inventory
     * open. The returned handle reflects live state and may become stale if
     * the player closes the menu.
     */
    Optional<Inventory> openInventory();

    /**
     * Closes whatever container the player has open and returns them to
     * their own inventory. No-op if no container is open. Marshals to the
     * server thread.
     */
    void closeInventory();
}
