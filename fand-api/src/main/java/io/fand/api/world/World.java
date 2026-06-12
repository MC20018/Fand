package io.fand.api.world;

import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentMap;
import io.fand.api.entity.Player;
import io.fand.api.entity.Entity;
import io.fand.api.entity.EntityKey;
import io.fand.api.entity.EntitySpawnOptions;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.EntityTypes;
import io.fand.api.entity.ItemEntity;
import io.fand.api.component.DataComponentKey;
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.ItemTypes;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.sound.SoundEffect;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;

/**
 * A loaded world (dimension) on the server. Identified by a {@link Key} matching
 * the underlying Minecraft dimension key (e.g. {@code minecraft:overworld}).
 *
 * <p>World handles are stable for as long as the dimension stays loaded; equality
 * is by {@link #key()}. Methods that touch world state must be called on the server
 * thread unless explicitly documented as thread-safe.
 *
 * <p>{@code World} is an Adventure {@link ForwardingAudience} that forwards to
 * the players currently in this world.
 */
public interface World extends ForwardingAudience {

    /** Dimension key, e.g. {@code minecraft:overworld}. */
    Key key();

    /** Convenience for {@code key().asString()}. */
    default String name() {
        return key().asString();
    }

    /** World seed. */
    long seed();

    /** Total game ticks elapsed for this world. */
    long gameTime();

    /** Sets total game ticks for this world. Marshals to the server thread. */
    CompletableFuture<Void> setGameTime(long ticks);

    /** Current default clock ticks for this world. */
    long time();

    /** Sets default clock ticks for this world. Marshals to the server thread. */
    CompletableFuture<Void> setTime(long ticks);

    /** Current server difficulty. */
    Difficulty difficulty();

    /** Sets server difficulty. Marshals to the server thread. */
    CompletableFuture<Void> setDifficulty(Difficulty difficulty);

    /** Whether rain is active. */
    boolean storm();

    /** Sets rain state. Marshals to the server thread. */
    CompletableFuture<Void> setStorm(boolean storm);

    /** Whether thunder is active. */
    boolean thundering();

    /** Sets thunder state. Marshals to the server thread. */
    CompletableFuture<Void> setThundering(boolean thundering);

    /** Live world border controls. */
    WorldBorder worldBorder();

    /** Biome key at block coordinates. */
    default Key biomeAt(int x, int y, int z) {
        return Key.key("minecraft", "plains");
    }

    /** Highest block Y using {@link HeightmapType#MOTION_BLOCKING}. */
    default int highestBlockYAt(int x, int z) {
        return highestBlockYAt(x, z, HeightmapType.MOTION_BLOCKING);
    }

    /** Highest block Y according to a vanilla heightmap. */
    default int highestBlockYAt(int x, int z, HeightmapType type) {
        java.util.Objects.requireNonNull(type, "type");
        return 0;
    }

    /** Global default spawn location. */
    default Location spawnLocation() {
        return at(0.0, 0.0, 0.0);
    }

    /** Sets the global default spawn location. Marshals to the server thread. */
    default CompletableFuture<Void> setSpawnLocation(Location location) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Spawn location changes are not supported"));
    }

    /** Reads a global vanilla game rule by id, e.g. {@code keepInventory}. */
    default Optional<String> gameRule(String name) {
        java.util.Objects.requireNonNull(name, "name");
        return Optional.empty();
    }

    /** Sets a global vanilla game rule from its command/string representation. */
    default CompletableFuture<Boolean> setGameRule(String name, String value) {
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(value, "value");
        return CompletableFuture.completedFuture(false);
    }

    /** Saves this world. Marshals to the server thread. */
    CompletableFuture<Boolean> save();

    /** Snapshot of all players currently in this world. */
    Collection<? extends Player> players();

    /** Snapshot of all loaded entities currently in this world, including players. */
    default Collection<? extends Entity> entities() {
        return java.util.List.of();
    }

    /** Snapshot of loaded entities of {@code type} in this world. */
    default Collection<? extends Entity> entities(EntityType type) {
        java.util.Objects.requireNonNull(type, "type");
        return entities().stream()
                .filter(entity -> entity.type().equals(type))
                .toList();
    }

    /** Convenience overload for generated vanilla entity keys. */
    default Collection<? extends Entity> entities(EntityKey type) {
        return entities(EntityTypes.of(type));
    }

    /** Looks up a loaded entity in this world by uuid. */
    default Optional<? extends Entity> entity(java.util.UUID uniqueId) {
        return Optional.empty();
    }

    /**
     * Snapshot of loaded entities within {@code radius} blocks of {@code center}.
     *
     * @throws IllegalArgumentException if {@code center} belongs to another world
     *         or {@code radius} is negative
     */
    default Collection<? extends Entity> nearbyEntities(Location center, double radius) {
        return java.util.List.of();
    }

    /**
     * Snapshot of loaded entities of {@code type} within {@code radius} blocks
     * of {@code center}.
     */
    default Collection<? extends Entity> nearbyEntities(Location center, double radius, EntityType type) {
        java.util.Objects.requireNonNull(type, "type");
        return nearbyEntities(center, radius).stream()
                .filter(entity -> entity.type().equals(type))
                .toList();
    }

    /** Convenience overload for generated vanilla entity keys. */
    default Collection<? extends Entity> nearbyEntities(Location center, double radius, EntityKey type) {
        return nearbyEntities(center, radius, EntityTypes.of(type));
    }

    /**
     * Snapshot of loaded entities whose bounds intersect the axis-aligned box
     * formed by {@code min} and {@code max}.
     */
    default Collection<? extends Entity> entitiesInBox(Location min, Location max) {
        requireSameWorld(min, this, "min");
        requireSameWorld(max, this, "max");
        double minX = Math.min(min.x(), max.x());
        double minY = Math.min(min.y(), max.y());
        double minZ = Math.min(min.z(), max.z());
        double maxX = Math.max(min.x(), max.x());
        double maxY = Math.max(min.y(), max.y());
        double maxZ = Math.max(min.z(), max.z());
        return entities().stream()
                .filter(entity -> intersects(entity, minX, minY, minZ, maxX, maxY, maxZ))
                .toList();
    }

    /** Snapshot of loaded entities of {@code type} whose bounds intersect the axis-aligned box. */
    default Collection<? extends Entity> entitiesInBox(Location min, Location max, EntityType type) {
        java.util.Objects.requireNonNull(type, "type");
        return entitiesInBox(min, max).stream()
                .filter(entity -> entity.type().equals(type))
                .toList();
    }

    /** Convenience overload for generated vanilla entity keys. */
    default Collection<? extends Entity> entitiesInBox(Location min, Location max, EntityKey type) {
        return entitiesInBox(min, max, EntityTypes.of(type));
    }

    /** Counts loaded entities whose bounds intersect the axis-aligned box. */
    default int countEntitiesInBox(Location min, Location max) {
        return entitiesInBox(min, max).size();
    }

    /** Counts loaded entities of {@code type} whose bounds intersect the axis-aligned box. */
    default int countEntitiesInBox(Location min, Location max, EntityType type) {
        java.util.Objects.requireNonNull(type, "type");
        return entitiesInBox(min, max, type).size();
    }

    /** Convenience overload for generated vanilla entity keys. */
    default int countEntitiesInBox(Location min, Location max, EntityKey type) {
        return countEntitiesInBox(min, max, EntityTypes.of(type));
    }

    /**
     * Finds one loaded entity whose bounds intersect the axis-aligned box.
     *
     * <p>The chosen entity is implementation-defined and should only be used
     * when any matching entity is acceptable.
     */
    default Optional<? extends Entity> firstEntityInBox(Location min, Location max) {
        return entitiesInBox(min, max).stream().findFirst();
    }

    /** Finds one loaded entity of {@code type} whose bounds intersect the axis-aligned box. */
    default Optional<? extends Entity> firstEntityInBox(Location min, Location max, EntityType type) {
        java.util.Objects.requireNonNull(type, "type");
        return entitiesInBox(min, max, type).stream().findFirst();
    }

    /** Convenience overload for generated vanilla entity keys. */
    default Optional<? extends Entity> firstEntityInBox(Location min, Location max, EntityKey type) {
        return firstEntityInBox(min, max, EntityTypes.of(type));
    }

    /**
     * Visits loaded entities whose bounds intersect the axis-aligned box without
     * requiring the caller to allocate a returned snapshot collection.
     */
    default void forEachEntityInBox(Location min, Location max, Consumer<? super Entity> action) {
        java.util.Objects.requireNonNull(action, "action");
        for (var entity : entitiesInBox(min, max)) {
            action.accept(entity);
        }
    }

    /** Visits loaded entities of {@code type} whose bounds intersect the axis-aligned box. */
    default void forEachEntityInBox(Location min, Location max, EntityType type, Consumer<? super Entity> action) {
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(action, "action");
        for (var entity : entitiesInBox(min, max, type)) {
            action.accept(entity);
        }
    }

    /** Convenience overload for generated vanilla entity keys. */
    default void forEachEntityInBox(Location min, Location max, EntityKey type, Consumer<? super Entity> action) {
        forEachEntityInBox(min, max, EntityTypes.of(type), action);
    }

    /** Finds the nearest loaded entity within {@code radius} blocks of {@code center}. */
    default Optional<? extends Entity> nearestEntity(Location center, double radius) {
        requireSameWorld(center, this, "center");
        requireNonNegativeFinite(radius, "radius");
        double radiusSquared = radius * radius;
        return nearbyEntities(center, radius).stream()
                .filter(entity -> {
                    var location = entity.location();
                    return location.world().key().equals(key()) && distanceSquared(center, location) <= radiusSquared;
                })
                .min(java.util.Comparator.comparingDouble(entity -> distanceSquared(center, entity.location())));
    }

    /** Finds the nearest loaded entity of {@code type} within {@code radius} blocks of {@code center}. */
    default Optional<? extends Entity> nearestEntity(Location center, double radius, EntityType type) {
        requireSameWorld(center, this, "center");
        requireNonNegativeFinite(radius, "radius");
        java.util.Objects.requireNonNull(type, "type");
        double radiusSquared = radius * radius;
        return nearbyEntities(center, radius, type).stream()
                .filter(entity -> distanceSquared(center, entity.location()) <= radiusSquared)
                .min(java.util.Comparator.comparingDouble(entity -> distanceSquared(center, entity.location())));
    }

    /** Convenience overload for generated vanilla entity keys. */
    default Optional<? extends Entity> nearestEntity(Location center, double radius, EntityKey type) {
        return nearestEntity(center, radius, EntityTypes.of(type));
    }

    /** Ray traces blocks using collider shapes and ignoring fluids. */
    default Optional<BlockRayTraceResult> rayTraceBlock(Location start, Vector3 direction, double maxDistance) {
        return rayTraceBlock(start, direction, maxDistance, RayTraceBlockMode.COLLIDER, RayTraceFluidMode.NONE);
    }

    /** Ray traces blocks from {@code start} along {@code direction}. */
    default Optional<BlockRayTraceResult> rayTraceBlock(
            Location start,
            Vector3 direction,
            double maxDistance,
            RayTraceBlockMode blockMode,
            RayTraceFluidMode fluidMode
    ) {
        return Optional.empty();
    }

    /** Ray traces entities from {@code start} along {@code direction}. */
    default Optional<EntityRayTraceResult> rayTraceEntity(Location start, Vector3 direction, double maxDistance) {
        return Optional.empty();
    }

    /**
     * Ray traces entities asynchronously when the implementation supports it.
     *
     * <p>Implementations that are backed by a game world may still execute the
     * trace on the server thread so entity state is read safely.
     */
    default CompletableFuture<Optional<EntityRayTraceResult>> rayTraceEntityAsync(
            Location start,
            Vector3 direction,
            double maxDistance
    ) {
        return CompletableFuture.completedFuture(rayTraceEntity(start, direction, maxDistance));
    }

    /** Ray traces entities of {@code type} from {@code start} along {@code direction}. */
    default Optional<EntityRayTraceResult> rayTraceEntity(
            Location start,
            Vector3 direction,
            double maxDistance,
            EntityType type
    ) {
        java.util.Objects.requireNonNull(type, "type");
        return rayTraceEntity(start, direction, maxDistance)
                .filter(result -> result.entity().type().equals(type));
    }

    /**
     * Asynchronously ray traces entities of {@code type}. See
     * {@link #rayTraceEntityAsync(Location, Vector3, double)} for scheduling
     * and thread-safety semantics.
     */
    default CompletableFuture<Optional<EntityRayTraceResult>> rayTraceEntityAsync(
            Location start,
            Vector3 direction,
            double maxDistance,
            EntityType type
    ) {
        java.util.Objects.requireNonNull(type, "type");
        return rayTraceEntityAsync(start, direction, maxDistance)
                .thenApply(result -> result.filter(hit -> hit.entity().type().equals(type)));
    }

    /** Convenience overload for generated vanilla entity keys. */
    default Optional<EntityRayTraceResult> rayTraceEntity(
            Location start,
            Vector3 direction,
            double maxDistance,
            EntityKey type
    ) {
        return rayTraceEntity(start, direction, maxDistance, EntityTypes.of(type));
    }

    /** Convenience overload for generated vanilla entity keys. */
    default CompletableFuture<Optional<EntityRayTraceResult>> rayTraceEntityAsync(
            Location start,
            Vector3 direction,
            double maxDistance,
            EntityKey type
    ) {
        return rayTraceEntityAsync(start, direction, maxDistance, EntityTypes.of(type));
    }

    /**
     * Spawns an entity of {@code type} at {@code location}. The future completes
     * with the spawned entity, or empty when vanilla cannot create/spawn that
     * type in this world.
     */
    default CompletableFuture<java.util.Optional<? extends Entity>> spawnEntity(Location location, EntityType type) {
        return spawnEntity(location, type, EntitySpawnOptions.defaults());
    }

    /**
     * Spawns an entity and applies {@code options} immediately after creation.
     * Options that do not apply to the spawned entity type are ignored.
     */
    default CompletableFuture<java.util.Optional<? extends Entity>> spawnEntity(
            Location location,
            EntityType type,
            EntitySpawnOptions options
    ) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Entity spawning is not supported"));
    }

    /** Convenience overload for generated vanilla entity keys. */
    default CompletableFuture<java.util.Optional<? extends Entity>> spawnEntity(Location location, EntityKey type) {
        return spawnEntity(location, EntityTypes.of(type));
    }

    /** Convenience overload for generated vanilla entity keys with spawn options. */
    default CompletableFuture<java.util.Optional<? extends Entity>> spawnEntity(
            Location location,
            EntityKey type,
            EntitySpawnOptions options
    ) {
        return spawnEntity(location, EntityTypes.of(type), options);
    }

    /**
     * Drops an item stack at {@code location}. The future completes with the
     * dropped item entity, or empty when {@code item} is empty.
     */
    default CompletableFuture<java.util.Optional<? extends ItemEntity>> dropItem(Location location, ItemStack item) {
        return dropItem(location, item, EntitySpawnOptions.defaults());
    }

    /**
     * Drops an item stack and applies item/common entity options immediately
     * after creation.
     */
    default CompletableFuture<java.util.Optional<? extends ItemEntity>> dropItem(
            Location location,
            ItemStack item,
            EntitySpawnOptions options
    ) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Item dropping is not supported"));
    }

    /** Convenience overload for dropping a plain item stack. */
    default CompletableFuture<java.util.Optional<? extends ItemEntity>> dropItem(Location location, ItemType type, int amount) {
        return dropItem(location, type.stack(amount));
    }

    /** Convenience overload for dropping a plain item stack with spawn options. */
    default CompletableFuture<java.util.Optional<? extends ItemEntity>> dropItem(
            Location location,
            ItemType type,
            int amount,
            EntitySpawnOptions options
    ) {
        return dropItem(location, type.stack(amount), options);
    }

    /** Convenience overload for generated vanilla item keys. */
    default CompletableFuture<java.util.Optional<? extends ItemEntity>> dropItem(Location location, ItemKey type, int amount) {
        return dropItem(location, ItemTypes.of(type), amount);
    }

    /** Convenience overload for generated vanilla item keys with spawn options. */
    default CompletableFuture<java.util.Optional<? extends ItemEntity>> dropItem(
            Location location,
            ItemKey type,
            int amount,
            EntitySpawnOptions options
    ) {
        return dropItem(location, ItemTypes.of(type), amount, options);
    }

    /** Plays a sound at {@code location} for players in this world. Marshals to the server thread. */
    void playSound(Location location, SoundEffect sound);

    /** Spawns a single particle at {@code location} for players in this world. Marshals to the server thread. */
    default void spawnParticle(Location location, ParticleEffect effect) {
        spawnParticle(location, effect, ParticleEmission.SINGLE);
    }

    /** Spawns particles at {@code location} for players in this world. Marshals to the server thread. */
    void spawnParticle(Location location, ParticleEffect effect, ParticleEmission emission);

    /** Strikes real lightning at {@code location}. Marshals to the server thread. */
    default CompletableFuture<Optional<? extends Entity>> strikeLightning(Location location) {
        return strikeLightning(location, false);
    }

    /**
     * Strikes lightning at {@code location}. When {@code visualOnly} is true,
     * vanilla only plays the visual/sound effect.
     */
    default CompletableFuture<Optional<? extends Entity>> strikeLightning(Location location, boolean visualOnly) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Lightning is not supported"));
    }

    /** Creates a block-breaking explosion without fire. Marshals to the server thread. */
    default CompletableFuture<Void> createExplosion(Location location, float power) {
        return createExplosion(location, power, false, true);
    }

    /** Creates an explosion. Marshals to the server thread. */
    default CompletableFuture<Void> createExplosion(Location location, float power, boolean fire, boolean breakBlocks) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Explosions are not supported"));
    }

    /** Whether the chunk at chunk coordinates {@code chunkX}, {@code chunkZ} is currently loaded. */
    default boolean chunkLoaded(int chunkX, int chunkZ) {
        return false;
    }

    /** Loads or generates the chunk at chunk coordinates. Marshals to the server thread. */
    default CompletableFuture<Boolean> loadChunk(int chunkX, int chunkZ) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Requests that a chunk may unload by clearing forced-load state.
     *
     * <p>The returned value is whether the forced-load state changed; the chunk
     * may remain loaded because of players, tickets, or pending server work.
     */
    default CompletableFuture<Boolean> unloadChunk(int chunkX, int chunkZ) {
        return setChunkForceLoaded(chunkX, chunkZ, false);
    }

    /** Whether this chunk is explicitly force-loaded. */
    default boolean chunkForceLoaded(int chunkX, int chunkZ) {
        return false;
    }

    /** Sets explicit force-loaded state for a chunk. Marshals to the server thread. */
    default CompletableFuture<Boolean> setChunkForceLoaded(int chunkX, int chunkZ, boolean forceLoaded) {
        return CompletableFuture.completedFuture(false);
    }

    /** Number of loaded entities in this world. */
    default int loadedEntityCount() {
        return entities().size();
    }

    /** Number of loaded entities whose current bounds intersect a loaded chunk. */
    default int entityCount(int chunkX, int chunkZ) {
        return 0;
    }

    /** Snapshot of loaded entities whose current bounds intersect a loaded chunk. */
    default Collection<? extends Entity> entitiesInChunk(int chunkX, int chunkZ) {
        return java.util.List.of();
    }

    /** Lightweight chunk state snapshot. */
    default ChunkSnapshot chunkSnapshot(int chunkX, int chunkZ) {
        return new ChunkSnapshot(this, chunkX, chunkZ, chunkLoaded(chunkX, chunkZ), chunkForceLoaded(chunkX, chunkZ), entityCount(chunkX, chunkZ));
    }

    /** Asynchronous lightweight chunk state snapshot. */
    default CompletableFuture<ChunkSnapshot> chunkSnapshotAsync(int chunkX, int chunkZ) {
        return CompletableFuture.completedFuture(chunkSnapshot(chunkX, chunkZ));
    }

    /** Snapshot of persisted component-bearing blocks in this world. */
    default Collection<? extends io.fand.api.block.Block> blocksWith(DataComponentKey<?> key) {
        java.util.Objects.requireNonNull(key, "key");
        return java.util.List.of();
    }

    /** Snapshot of persisted component-bearing blocks in a chunk. */
    default Collection<? extends io.fand.api.block.Block> blocksWith(DataComponentKey<?> key, int chunkX, int chunkZ) {
        java.util.Objects.requireNonNull(key, "key");
        return java.util.List.of();
    }

    /**
     * Applies block changes asynchronously. Implementations backed by a live
     * Minecraft world must perform the actual mutation on the server thread.
     */
    default CompletableFuture<BlockBatchResult> setBlocks(Collection<BlockBatchChange> changes) {
        return setBlocks(changes, BlockBatchOptions.defaults());
    }

    /**
     * Applies block changes asynchronously using the supplied scheduling and
     * update policy.
     */
    default CompletableFuture<BlockBatchResult> setBlocks(
            Collection<BlockBatchChange> changes,
            BlockBatchOptions options
    ) {
        java.util.Objects.requireNonNull(changes, "changes");
        java.util.Objects.requireNonNull(options, "options");
        if (changes.isEmpty()) {
            return CompletableFuture.completedFuture(BlockBatchResult.empty());
        }
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Batch block operations are not supported"));
    }

    /** Fills the inclusive cuboid formed by {@code min} and {@code max}. */
    default CompletableFuture<BlockBatchResult> fillBlocks(Location min, Location max, BlockType type) {
        return fillBlocks(min, max, type, DataComponentMap.EMPTY, BlockBatchOptions.defaults());
    }

    /** Fills the inclusive cuboid formed by {@code min} and {@code max}. */
    default CompletableFuture<BlockBatchResult> fillBlocks(
            Location min,
            Location max,
            BlockType type,
            DataComponentMap components,
            BlockBatchOptions options
    ) {
        requireSameWorld(min, this, "min");
        requireSameWorld(max, this, "max");
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(components, "components");
        java.util.Objects.requireNonNull(options, "options");
        int minX = Math.min(min.blockX(), max.blockX());
        int minY = Math.min(min.blockY(), max.blockY());
        int minZ = Math.min(min.blockZ(), max.blockZ());
        int maxX = Math.max(min.blockX(), max.blockX());
        int maxY = Math.max(min.blockY(), max.blockY());
        int maxZ = Math.max(min.blockZ(), max.blockZ());
        long requested = BlockBatchVolumes.cappedVolume(minX, minY, minZ, maxX, maxY, maxZ);
        if (requested > Integer.MAX_VALUE) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("batch contains too many blocks: " + requested));
        }
        var changes = new FillBlockChanges(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                type,
                components,
                (int) requested);
        return setBlocks(changes, options);
    }

    /** Pastes an in-memory relative block clipboard at {@code origin}. */
    default CompletableFuture<BlockBatchResult> pasteBlocks(Location origin, BlockClipboard clipboard) {
        return pasteBlocks(origin, clipboard, BlockBatchOptions.defaults());
    }

    /** Pastes an in-memory relative block clipboard at {@code origin}. */
    default CompletableFuture<BlockBatchResult> pasteBlocks(
            Location origin,
            BlockClipboard clipboard,
            BlockBatchOptions options
    ) {
        requireSameWorld(origin, this, "origin");
        java.util.Objects.requireNonNull(clipboard, "clipboard");
        java.util.Objects.requireNonNull(options, "options");
        var changes = new java.util.ArrayList<BlockBatchChange>(clipboard.blocks().size());
        int originX = origin.blockX();
        int originY = origin.blockY();
        int originZ = origin.blockZ();
        for (var block : clipboard.blocks()) {
            changes.add(block.offset(originX, originY, originZ));
        }
        return setBlocks(changes, options);
    }

    /** Builds a {@link Location} in this world. */
    default Location at(double x, double y, double z) {
        return new Location(this, x, y, z, 0.0F, 0.0F);
    }

    /** Builds a {@link Location} in this world with rotation. */
    default Location at(double x, double y, double z, float yaw, float pitch) {
        return new Location(this, x, y, z, yaw, pitch);
    }

    /**
     * Returns a positional block handle. The handle is lazy — it does not read
     * the world until {@link io.fand.api.block.Block#type()} or
     * {@link io.fand.api.block.Block#setType} is invoked.
     */
    io.fand.api.block.Block blockAt(int x, int y, int z);

    private static void requireSameWorld(Location location, World world, String name) {
        java.util.Objects.requireNonNull(location, name);
        if (!location.world().key().equals(world.key())) {
            throw new IllegalArgumentException(name + " world " + location.world().key().asString()
                    + " does not match " + world.key().asString());
        }
        if (!Double.isFinite(location.x()) || !Double.isFinite(location.y()) || !Double.isFinite(location.z())) {
            throw new IllegalArgumentException(name + " coordinates must be finite");
        }
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }

    private static boolean intersects(
            Entity entity,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        var location = entity.location();
        double halfWidth = Math.max(0.0, entity.width()) * 0.5;
        double height = Math.max(0.0, entity.height());
        return location.x() + halfWidth >= minX
                && location.x() - halfWidth <= maxX
                && location.y() + height >= minY
                && location.y() <= maxY
                && location.z() + halfWidth >= minZ
                && location.z() - halfWidth <= maxZ;
    }

    private static double distanceSquared(Location a, Location b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }

}
