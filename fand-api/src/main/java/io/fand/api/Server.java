package io.fand.api;

import io.fand.api.command.CommandRegistry;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customitem.CustomItemRegistry;
import io.fand.api.advancement.AdvancementRegistry;
import io.fand.api.enchantment.EnchantmentRegistry;
import io.fand.api.entity.EntityKey;
import io.fand.api.entity.Player;
import io.fand.api.event.EventBus;
import io.fand.api.gui.GuiService;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.loot.LootTableService;
import io.fand.api.map.MapService;
import io.fand.api.messaging.PluginMessaging;
import io.fand.api.performance.ServerPerformance;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.permission.PermissionService;
import io.fand.api.player.PlayerAccessService;
import io.fand.api.plugin.PluginManager;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.structure.StructureService;
import io.fand.api.tag.TagRegistry;
import io.fand.api.world.World;
import io.fand.api.world.WorldCreateOptions;
import io.fand.api.world.WorldTemplate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Top-level handle to a running Fand server instance.
 *
 * <p>Obtain the singleton via {@link Fand#server()}. The instance is bound during
 * server bootstrap and remains valid for the lifetime of the JVM.
 *
 * <p>{@code Server} is an Adventure {@link ForwardingAudience} that forwards to
 * every online player. {@code server().sendMessage(component)} broadcasts to
 * everyone; {@code server().showTitle(title)} shows a title to everyone; etc.
 */
public interface Server extends ForwardingAudience {

    /** Server brand identifier reported to clients. */
    String brand();

    /** Running Fand version (e.g. {@code 0.1.0-SNAPSHOT}). */
    String version();

    /** Minecraft protocol version this server implements. */
    String minecraftVersion();

    /** Plugin lifecycle and lookup. */
    PluginManager plugins();

    /** Global event dispatcher. */
    EventBus events();

    /** Global permission service. */
    PermissionService permissions();

    /** Global command registry. */
    CommandRegistry commands();

    /** Global recipe registry. Registration and removal marshal to the server thread. */
    RecipeRegistry recipes();

    /** Global persistent vanilla scoreboard service. */
    ScoreboardService scoreboard();

    /** Global packet registry. Prefer {@link io.fand.api.plugin.PluginContext#packets()} for plugin-owned registrations. */
    PacketRegistry packets();

    /** Global custom item registry. Prefer {@link io.fand.api.plugin.PluginContext#customItems()} for plugin-owned registrations. */
    CustomItemRegistry customItems();

    /** Global custom block registry. Prefer {@link io.fand.api.plugin.PluginContext#customBlocks()} for plugin-owned registrations. */
    CustomBlockRegistry customBlocks();

    /** Read-only view of vanilla data-pack tags for blocks, items, entities, fluids, and damage types. */
    default TagRegistry tags() {
        return TagRegistry.empty();
    }

    default LootTableService lootTables() {
        return LootTableService.empty();
    }

    default AdvancementRegistry advancements() {
        return AdvancementRegistry.empty();
    }

    default EnchantmentRegistry enchantments() {
        return EnchantmentRegistry.empty();
    }

    default StructureService structures() {
        return StructureService.empty();
    }

    default MapService maps() {
        return MapService.empty();
    }

    default PluginMessaging pluginMessaging() {
        return PluginMessaging.empty();
    }

    /** Lightweight GUI routing service. */
    GuiService guis();

    /** Main-thread and async task scheduler. */
    Scheduler scheduler();

    /** Currently online player count. */
    int onlinePlayers();

    /** Configured maximum simultaneous players, or {@code -1} for uncapped. */
    int maxPlayers();

    /** Latest published server tick performance snapshot. */
    ServerPerformance performance();

    default int currentTick() {
        return (int) Math.min(Integer.MAX_VALUE, performance().tickCount());
    }

    default String motd() {
        return "";
    }

    default void setMotd(String motd) {
        throw new UnsupportedOperationException("MOTD changes are not supported");
    }

    default CompletableFuture<Boolean> reloadData() {
        return CompletableFuture.completedFuture(false);
    }

    /** Snapshot of all currently online players. The returned collection is a copy. */
    Collection<? extends Player> players();

    /** Profile lookup and server access-list controls for offline and online players. */
    PlayerAccessService playerAccess();

    /** Looks up an online player by uuid. */
    Optional<? extends Player> player(UUID uniqueId);

    /** Looks up an online player by exact (case-sensitive) name. */
    Optional<? extends Player> player(String name);

    /** Looks up a loaded entity by uuid across all worlds, including players. */
    Optional<? extends io.fand.api.entity.Entity> entity(UUID uniqueId);

    /** Snapshot of all loaded worlds. The returned collection is a copy. */
    Collection<? extends World> worlds();

    /** Looks up a loaded world by dimension key (e.g. {@code minecraft:overworld}). */
    Optional<? extends World> world(Key key);

    /** The default (overworld) world. Present once the server has finished loading. */
    Optional<? extends World> defaultWorld();

    /**
     * Creates and loads a dynamic world from a vanilla generation template.
     *
     * <p>The operation marshals to the server thread. The returned future fails
     * when the key is already loaded, the server is not running, or the selected
     * template is unavailable in the active dimension registry.
     */
    CompletableFuture<? extends World> createWorld(Key key, WorldTemplate template);

    /**
     * Creates and loads a dynamic world using explicit generation options.
     *
     * <p>Use {@link WorldCreateOptions#voidWorld()} for empty worlds and
     * {@link WorldCreateOptions#generated(io.fand.api.world.WorldGenerator)}
     * for plugin-provided chunk generation.
     */
    default CompletableFuture<? extends World> createWorld(Key key, WorldCreateOptions options) {
        return createWorld(key, options.template());
    }

    /**
     * Saves and unloads a dynamic world. Vanilla base dimensions cannot be
     * unloaded, and worlds with players are rejected.
     *
     * <p>The operation marshals to the server thread and fires
     * {@link io.fand.api.event.world.WorldUnloadEvent} before the level is
     * removed.
     */
    CompletableFuture<Boolean> unloadWorld(Key key);

    /** Convenience for {@code unloadWorld(world.key())}. */
    default CompletableFuture<Boolean> unloadWorld(World world) {
        return unloadWorld(world.key());
    }

    /** Looks up a block type by its registry key. */
    Optional<? extends io.fand.api.block.BlockType> blockType(Key key);

    /** Looks up an item type by its registry key. */
    Optional<? extends io.fand.api.item.ItemType> itemType(Key key);

    /** Looks up an entity type by its registry key. */
    Optional<? extends io.fand.api.entity.EntityType> entityType(Key key);

    /** Convenience overload for generated vanilla entity keys. */
    default Optional<? extends io.fand.api.entity.EntityType> entityType(EntityKey key) {
        return entityType(key.key());
    }

    /** Current lifecycle phase. */
    LifecyclePhase phase();

    /** Initiates an orderly shutdown. */
    void shutdown(@Nullable String reason);

    /**
     * Creates a new server-side {@link io.fand.api.inventory.Inventory} of
     * the given type, size, and title. Used by
     * {@link io.fand.api.inventory.Inventories} — plugins should usually
     * call that instead.
     *
     * @throws IllegalArgumentException if {@code size} is invalid for
     *         {@code type}, or if {@code type} is not standalone-openable
     */
    io.fand.api.inventory.Inventory createInventory(
            io.fand.api.inventory.InventoryType type,
            int size,
            net.kyori.adventure.text.Component title);
}
