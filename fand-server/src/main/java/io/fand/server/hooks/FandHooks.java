package io.fand.server.hooks;

import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventListener;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Player;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.performance.MetricStatistics;
import io.fand.api.performance.ServerPerformance;
import io.fand.api.performance.TickWindow;
import io.fand.api.performance.TickWindowSnapshot;
import io.fand.api.player.PlayerProfile;
import io.fand.server.chunk.ChunkSendScheduler;
import io.fand.server.chunk.ChunkTrackingSnapshot;
import io.fand.server.chunk.AsyncChunkPacketSender;
import io.fand.server.FandServer;
import io.fand.server.Main;
import io.fand.server.entity.EntityRegistry;
import io.fand.server.entity.FandEntity;
import io.fand.server.entity.FandPlayer;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.network.ForwardedPlayerInfo;
import io.fand.server.network.ProxyForwarding;
import io.fand.server.network.ProxyForwardingMode;
import io.fand.server.network.VelocityForwardingQueryAnswerPayload;
import io.fand.server.world.FandWorld;
import io.fand.server.world.WorldRegistry;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static facade used by patched vanilla code to talk to the Fand runtime.
 *
 * <p>Hot paths (player move, ~20Hz per player) call several runtime accessors;
 * each direct {@code Main.runtime()} read is a volatile load. Centralising
 * those calls here also gives patch sites a single typed entry point that is
 * easier to grep for, and concentrates the {@code Main.runtime()} surface so
 * any future refactor (e.g. dependency injection) only needs to touch this
 * class rather than every patched vanilla file.
 *
 * <p>All accessors return {@code Optional} or {@code null}-tolerant results so
 * patch sites can safely run before {@link FandServer#attach attach} (which
 * wires the world/entity registries).
 */
public final class FandHooks {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandHooks.class);
    private static final EventSubscription NOOP_SUBSCRIPTION = new EventSubscription() {
        @Override
        public boolean active() {
            return false;
        }

        @Override
        public void unregister() {
        }
    };
    private static final EventBus NOOP_EVENTS = new EventBus() {
        @Override
        public <E extends Event> EventSubscription subscribe(Class<E> type, EventPriority priority, EventListener<E> listener) {
            return NOOP_SUBSCRIPTION;
        }

        @Override
        public <E extends Event> E fire(E event) {
            return event;
        }

        @Override
        public boolean hasListeners(Class<? extends Event> type) {
            return false;
        }

        @Override
        public <E extends Event> CompletableFuture<E> fireAsync(E event, Executor executor) {
            return CompletableFuture.completedFuture(event);
        }
    };
    private static final ServerPerformance EMPTY_PERFORMANCE = emptyPerformance();
    private static final io.fand.server.block.FandCustomBlockRegistry NOOP_CUSTOM_BLOCKS =
            new io.fand.server.block.FandCustomBlockRegistry(NOOP_EVENTS);
    private static final io.fand.server.console.gui.GuiThemeService FALLBACK_GUI_THEMES =
            new io.fand.server.console.gui.GuiThemeService(io.fand.server.console.gui.GuiTheme.SYSTEM);

    // Pushed by FandServer on config load/reload. Static volatiles (rather than
    // a runtime lookup) because these gate per-collision-pair and per-explosion
    // vanilla code where even an extra pointer chase is measurable. Defaults
    // mirror FandConfig.Performance so behaviour before attach matches a
    // default config.
    private static volatile boolean playerSpeedCheck = true;
    private static volatile boolean playerCommandLogging = true;
    private static volatile boolean validateLoginUsernames = true;
    private static volatile boolean explosionDensityCacheEnabled = true;
    private static volatile boolean collisionTeamCacheEnabled = true;
    private static volatile boolean explosionBlockCacheEnabled = true;
    private static volatile int tntDetonationBudget = 0;
    private static volatile boolean explosionDropHashMerge = true;
    private static volatile boolean explosionExposureClipCache = true;
    private static volatile boolean explosionEntityCache = true;
    private static volatile boolean entityHardCollisionCandidateIndex = true;
    private static volatile boolean entitySectionChunkScan = true;
    private static volatile boolean entityCollisionAbortPropagation = true;
    private static volatile boolean pushableEntityConsumer = true;
    private static volatile boolean entityMovementLazyColliders = true;
    private static volatile boolean entityTrackerFastPath = true;
    private static volatile boolean deepPassengerIteration = true;
    private static volatile boolean entityTypeLookupFastPath = true;
    private static volatile boolean randomTickPositionMask = true;
    private static volatile boolean fluidTickContainerQueue = true;
    private static volatile boolean flowingFluidSpreadArray = true;
    private static volatile boolean chunkGenerationTaskPlanCache = true;
    private static volatile boolean chunkTaskDispatcherBatchLoop = true;
    private static volatile boolean chunkStorageRegionScanFastPath = true;
    private static volatile boolean worldgenSeaLevelCache = true;
    private static volatile boolean itemEntityMergeFastPath = true;
    private static volatile boolean areaEffectCloudFastPath = true;
    private static volatile boolean aiNearestTargetFastPath = true;
    private static volatile boolean aiGoalStreamFastPath = true;
    private static volatile boolean aiSensorLoopFastPath = true;
    private static volatile boolean playerNameLookupIndex = true;
    private static volatile boolean scoreboardTeamWaypointFastPath = true;
    private static volatile boolean reusablePacketEncoding = true;
    private static volatile boolean packetFlushCoalescing = true;
    private static volatile boolean outboundPacketQueueCoalescing = true;
    private static volatile int chargedProjectilesSoftLimit = 1024;
    private static volatile int bundleContentsSoftLimit = 256;
    private static volatile boolean asyncChunkPacketPreparation = false;
    private static volatile int chunkWorldgenParallelism = 0;
    private static volatile boolean chunkDedicatedLightThread = true;
    private static volatile boolean chunkLightTaskQueueFastPath = true;
    private static volatile boolean chunkTeleportPreload = true;
    private static volatile int chunkTeleportPreloadExtraRadius = 3;
    private static volatile boolean chunkTeleportPreloadSimulation = true;
    private static volatile int chunkTeleportChunkSendBurstTicks = 40;
    private static volatile int chunkTeleportChunkSendBurstChunksPerTick = 64;
    private static volatile int chunkTeleportChunkSendBurstBatches = 10;
    private static volatile boolean zeroTickPlants = false;
    private static volatile boolean oldHopperSuckInBehavior = false;
    private static volatile boolean shearsInDispenserCanZeroAmount = false;
    private static volatile boolean allowEntityPortalWithPassenger = true;
    private static volatile boolean disableGatewayPortalEntityTicking = false;
    private static volatile boolean disableLivingEntityAiStepAliveCheck = false;
    private static volatile boolean spawnInvulnerableTime = false;
    private static volatile boolean oldZombiePiglinDrop = false;
    private static volatile boolean oldZombieReinforcement = false;
    private static volatile boolean allowAnvilDestroyItemEntities = false;
    private static volatile boolean disableItemDamageCheck = false;
    private static volatile boolean keepLeashConnectWhenUseFirework = false;
    private static volatile boolean tntWetExplosionNoItemDamage = false;
    private static volatile boolean oldProjectileExplosionBehavior = false;
    private static volatile boolean oldThrowableProjectileTickOrder = false;
    private static volatile boolean oldMinecartMotionBehavior = false;
    private static volatile boolean copperBulbOneGameTickDelay = false;
    private static volatile boolean crafterOneGameTickDelay = false;
    private static volatile boolean noTntPlaceUpdate = false;
    private static volatile boolean allowPistonDuplication = true;
    private static volatile boolean allowTntDuplication = true;
    private static volatile boolean allowRailDuplication = true;
    private static volatile boolean allowCarpetDuplication = true;
    private static volatile boolean allowGravityBlockEndPortalDuplication = true;
    private static volatile boolean redstoneIgnoreUpwardsUpdate = false;
    private static volatile boolean movableBuddingAmethyst = false;
    private static volatile boolean stringTripwireHookDuplicate = true;
    private static volatile int tripwireBehavior = 21;

    private FandHooks() {
    }

    public static void applyPerformanceConfig(io.fand.server.config.FandConfig.Performance performance) {
        explosionDensityCacheEnabled = performance.explosionDensityCache;
        collisionTeamCacheEnabled = performance.collisionTeamCache;
        explosionBlockCacheEnabled = performance.explosionBlockCache;
        tntDetonationBudget = performance.tntDetonationBudget;
        explosionDropHashMerge = performance.explosionDropHashMerge;
        explosionExposureClipCache = performance.explosionExposureClipCache;
        explosionEntityCache = performance.explosionEntityCache;
        entityHardCollisionCandidateIndex = performance.entityHardCollisionCandidateIndex;
        entitySectionChunkScan = performance.entitySectionChunkScan;
        entityCollisionAbortPropagation = performance.entityCollisionAbortPropagation;
        pushableEntityConsumer = performance.pushableEntityConsumer;
        entityMovementLazyColliders = performance.entityMovementLazyColliders;
        entityTrackerFastPath = performance.entityTrackerFastPath;
        deepPassengerIteration = performance.deepPassengerIteration;
        entityTypeLookupFastPath = performance.entityTypeLookupFastPath;
        randomTickPositionMask = performance.randomTickPositionMask;
        fluidTickContainerQueue = performance.fluidTickContainerQueue;
        flowingFluidSpreadArray = performance.flowingFluidSpreadArray;
        chunkGenerationTaskPlanCache = performance.chunkGenerationTaskPlanCache;
        chunkTaskDispatcherBatchLoop = performance.chunkTaskDispatcherBatchLoop;
        chunkStorageRegionScanFastPath = performance.chunkStorageRegionScanFastPath;
        worldgenSeaLevelCache = performance.worldgenSeaLevelCache;
        itemEntityMergeFastPath = performance.itemEntityMergeFastPath;
        areaEffectCloudFastPath = performance.areaEffectCloudFastPath;
        aiNearestTargetFastPath = performance.aiNearestTargetFastPath;
        aiGoalStreamFastPath = performance.aiGoalStreamFastPath;
        aiSensorLoopFastPath = performance.aiSensorLoopFastPath;
        playerNameLookupIndex = performance.playerNameLookupIndex;
        scoreboardTeamWaypointFastPath = performance.scoreboardTeamWaypointFastPath;
        reusablePacketEncoding = performance.reusablePacketEncoding;
        packetFlushCoalescing = performance.packetFlushCoalescing;
        outboundPacketQueueCoalescing = performance.outboundPacketQueueCoalescing;
        chargedProjectilesSoftLimit = performance.chargedProjectilesSoftLimit;
        bundleContentsSoftLimit = performance.bundleContentsSoftLimit;
    }

    public static void applyPlayerConfig(io.fand.server.config.FandConfig.Players players) {
        playerSpeedCheck = players.speedCheck;
        playerCommandLogging = players.logCommands;
    }

    public static void applyAuthenticationConfig(io.fand.server.config.FandConfig.Authentication authentication) {
        validateLoginUsernames = authentication.validateUsernames;
    }

    public static void applyChunkConfig(io.fand.server.config.FandConfig.Chunks chunks) {
        asyncChunkPacketPreparation = chunks.asyncChunkPacketPreparation;
        chunkWorldgenParallelism = chunks.worldgenParallelism;
        chunkDedicatedLightThread = chunks.dedicatedLightThread;
        chunkLightTaskQueueFastPath = chunks.lightTaskQueueFastPath;
        chunkTeleportPreload = chunks.teleportPreload;
        chunkTeleportPreloadExtraRadius = chunks.teleportPreloadExtraRadius;
        chunkTeleportPreloadSimulation = chunks.teleportPreloadSimulation;
        chunkTeleportChunkSendBurstTicks = chunks.teleportChunkSendBurstTicks;
        chunkTeleportChunkSendBurstChunksPerTick = chunks.teleportChunkSendBurstChunksPerTick;
        chunkTeleportChunkSendBurstBatches = chunks.teleportChunkSendBurstBatches;
        var runtime = activeRuntime();
        if (runtime != null) {
            runtime.asyncChunkPackets().reconfigure(chunks.asyncChunkPacketPreparation);
        }
    }

    public static boolean playerSpeedCheckEnabled() {
        return playerSpeedCheck;
    }

    public static boolean playerCommandLoggingEnabled() {
        return playerCommandLogging;
    }

    public static boolean validateLoginUsernames() {
        return validateLoginUsernames;
    }

    public static java.util.concurrent.Executor chunkBackgroundExecutor() {
        var runtime = activeRuntime();
        return runtime == null ? net.minecraft.util.Util.backgroundExecutor() : runtime.chunkBackgroundExecutor();
    }

    public static java.util.concurrent.Executor chunkWorldgenExecutor() {
        var runtime = activeRuntime();
        return runtime == null ? net.minecraft.util.Util.backgroundExecutor() : runtime.chunkWorldgenExecutor();
    }

    public static void applyTechnicalConfig(io.fand.server.config.FandConfig.Technical technical) {
        zeroTickPlants = technical.zeroTickPlants;
        oldHopperSuckInBehavior = technical.oldHopperSuckInBehavior;
        shearsInDispenserCanZeroAmount = technical.shearsInDispenserCanZeroAmount;
        allowEntityPortalWithPassenger = technical.allowEntityPortalWithPassenger;
        disableGatewayPortalEntityTicking = technical.disableGatewayPortalEntityTicking;
        disableLivingEntityAiStepAliveCheck = technical.disableLivingEntityAiStepAliveCheck;
        spawnInvulnerableTime = technical.spawnInvulnerableTime;
        oldZombiePiglinDrop = technical.oldZombiePiglinDrop;
        oldZombieReinforcement = technical.oldZombieReinforcement;
        allowAnvilDestroyItemEntities = technical.allowAnvilDestroyItemEntities;
        disableItemDamageCheck = technical.disableItemDamageCheck;
        keepLeashConnectWhenUseFirework = technical.keepLeashConnectWhenUseFirework;
        tntWetExplosionNoItemDamage = technical.tntWetExplosionNoItemDamage;
        oldProjectileExplosionBehavior = technical.oldProjectileExplosionBehavior;
        oldThrowableProjectileTickOrder = technical.oldThrowableProjectileTickOrder;
        oldMinecartMotionBehavior = technical.oldMinecartMotionBehavior;
        copperBulbOneGameTickDelay = technical.copperBulbOneGameTickDelay;
        crafterOneGameTickDelay = technical.crafterOneGameTickDelay;
        noTntPlaceUpdate = technical.noTntPlaceUpdate;
        allowPistonDuplication = technical.allowPistonDuplication;
        allowTntDuplication = technical.allowTntDuplication;
        allowRailDuplication = technical.allowRailDuplication;
        allowCarpetDuplication = technical.allowCarpetDuplication;
        allowGravityBlockEndPortalDuplication = technical.allowGravityBlockEndPortalDuplication;
        redstoneIgnoreUpwardsUpdate = technical.redstoneIgnoreUpwardsUpdate;
        movableBuddingAmethyst = technical.movableBuddingAmethyst;
        stringTripwireHookDuplicate = technical.stringTripwireHookDuplicate;
        tripwireBehavior = switch (technical.tripwireBehavior) {
            case "vanilla_20" -> 20;
            case "mixed" -> 0;
            default -> 21;
        };
    }

    public static boolean explosionDensityCacheEnabled() {
        return explosionDensityCacheEnabled;
    }

    public static boolean collisionTeamCacheEnabled() {
        return collisionTeamCacheEnabled;
    }

    public static boolean explosionBlockCacheEnabled() {
        return explosionBlockCacheEnabled;
    }

    public static int tntDetonationBudget() {
        return tntDetonationBudget;
    }

    public static boolean explosionDropHashMerge() {
        return explosionDropHashMerge;
    }

    public static boolean explosionExposureClipCacheEnabled() {
        return explosionExposureClipCache;
    }

    public static boolean explosionEntityCacheEnabled() {
        return explosionEntityCache;
    }

    public static boolean entityHardCollisionCandidateIndexEnabled() {
        return entityHardCollisionCandidateIndex;
    }

    public static boolean entitySectionChunkScanEnabled() {
        return entitySectionChunkScan;
    }

    public static boolean entityCollisionAbortPropagationEnabled() {
        return entityCollisionAbortPropagation;
    }

    public static boolean pushableEntityConsumerEnabled() {
        return pushableEntityConsumer;
    }

    public static boolean entityMovementLazyCollidersEnabled() {
        return entityMovementLazyColliders;
    }

    public static boolean entityTrackerFastPathEnabled() {
        return entityTrackerFastPath;
    }

    public static boolean deepPassengerIterationEnabled() {
        return deepPassengerIteration;
    }

    public static boolean entityTypeLookupFastPathEnabled() {
        return entityTypeLookupFastPath;
    }

    public static boolean randomTickPositionMaskEnabled() {
        return randomTickPositionMask;
    }

    public static boolean fluidTickContainerQueueEnabled() {
        return fluidTickContainerQueue;
    }

    public static boolean flowingFluidSpreadArrayEnabled() {
        return flowingFluidSpreadArray;
    }

    public static boolean chunkGenerationTaskPlanCacheEnabled() {
        return chunkGenerationTaskPlanCache;
    }

    public static boolean chunkTaskDispatcherBatchLoopEnabled() {
        return chunkTaskDispatcherBatchLoop;
    }

    public static boolean chunkStorageRegionScanFastPathEnabled() {
        return chunkStorageRegionScanFastPath;
    }

    public static boolean worldgenSeaLevelCacheEnabled() {
        return worldgenSeaLevelCache;
    }

    public static boolean itemEntityMergeFastPathEnabled() {
        return itemEntityMergeFastPath;
    }

    public static boolean areaEffectCloudFastPathEnabled() {
        return areaEffectCloudFastPath;
    }

    public static boolean aiNearestTargetFastPathEnabled() {
        return aiNearestTargetFastPath;
    }

    public static boolean aiGoalStreamFastPathEnabled() {
        return aiGoalStreamFastPath;
    }

    public static boolean aiSensorLoopFastPathEnabled() {
        return aiSensorLoopFastPath;
    }

    public static boolean playerNameLookupIndexEnabled() {
        return playerNameLookupIndex;
    }

    public static boolean scoreboardTeamWaypointFastPathEnabled() {
        return scoreboardTeamWaypointFastPath;
    }

    public static boolean reusablePacketEncodingEnabled() {
        return reusablePacketEncoding;
    }

    public static boolean packetFlushCoalescingEnabled() {
        return packetFlushCoalescing;
    }

    public static boolean outboundPacketQueueCoalescingEnabled() {
        return outboundPacketQueueCoalescing;
    }

    public static int chargedProjectilesSoftLimit() {
        return chargedProjectilesSoftLimit;
    }

    public static int bundleContentsSoftLimit() {
        return bundleContentsSoftLimit;
    }

    public static @Nullable AsyncChunkPacketSender asyncChunkPacketSender() {
        var runtime = activeRuntime();
        if (runtime == null || !asyncChunkPacketPreparation) {
            return null;
        }
        var sender = runtime.asyncChunkPackets();
        return sender.enabled() ? sender : null;
    }

    public static int chunkWorldgenParallelism() {
        int configured = chunkWorldgenParallelism;
        if (configured > 0) {
            return configured;
        }
        int processors = Runtime.getRuntime().availableProcessors();
        return Math.max(2, Math.min(8, processors / 2));
    }

    public static boolean chunkDedicatedLightThreadEnabled() {
        return chunkDedicatedLightThread;
    }

    public static boolean chunkLightTaskQueueFastPathEnabled() {
        return chunkLightTaskQueueFastPath;
    }

    public static boolean chunkTeleportPreloadEnabled() {
        return chunkTeleportPreload;
    }

    public static int chunkTeleportPreloadRadius(final int viewDistance, final int footprintRadius) {
        int baseRadius = Math.max(Math.max(1, viewDistance), footprintRadius);
        return baseRadius + Math.max(0, chunkTeleportPreloadExtraRadius);
    }

    public static boolean chunkTeleportPreloadSimulationEnabled() {
        return chunkTeleportPreloadSimulation;
    }

    public static int chunkTeleportChunkSendBurstTicks() {
        return chunkTeleportChunkSendBurstTicks;
    }

    public static int chunkTeleportChunkSendBurstChunksPerTick() {
        return chunkTeleportChunkSendBurstChunksPerTick;
    }

    public static int chunkTeleportChunkSendBurstBatches() {
        return chunkTeleportChunkSendBurstBatches;
    }

    public static boolean zeroTickPlantsEnabled() {
        return zeroTickPlants;
    }

    public static boolean oldHopperSuckInBehaviorEnabled() {
        return oldHopperSuckInBehavior;
    }

    public static boolean shearsInDispenserCanZeroAmountEnabled() {
        return shearsInDispenserCanZeroAmount;
    }

    public static boolean allowEntityPortalWithPassengerEnabled() {
        return allowEntityPortalWithPassenger;
    }

    public static boolean disableGatewayPortalEntityTickingEnabled() {
        return disableGatewayPortalEntityTicking;
    }

    public static boolean disableLivingEntityAiStepAliveCheckEnabled() {
        return disableLivingEntityAiStepAliveCheck;
    }

    public static boolean spawnInvulnerableTimeEnabled() {
        return spawnInvulnerableTime;
    }

    public static boolean oldZombiePiglinDropEnabled() {
        return oldZombiePiglinDrop;
    }

    public static boolean oldZombieReinforcementEnabled() {
        return oldZombieReinforcement;
    }

    public static boolean allowAnvilDestroyItemEntitiesEnabled() {
        return allowAnvilDestroyItemEntities;
    }

    public static boolean disableItemDamageCheckEnabled() {
        return disableItemDamageCheck;
    }

    public static boolean keepLeashConnectWhenUseFireworkEnabled() {
        return keepLeashConnectWhenUseFirework;
    }

    public static boolean tntWetExplosionNoItemDamageEnabled() {
        return tntWetExplosionNoItemDamage;
    }

    public static boolean oldProjectileExplosionBehaviorEnabled() {
        return oldProjectileExplosionBehavior;
    }

    public static boolean oldThrowableProjectileTickOrderEnabled() {
        return oldThrowableProjectileTickOrder;
    }

    public static boolean oldMinecartMotionBehaviorEnabled() {
        return oldMinecartMotionBehavior;
    }

    public static boolean copperBulbOneGameTickDelayEnabled() {
        return copperBulbOneGameTickDelay;
    }

    public static boolean crafterOneGameTickDelayEnabled() {
        return crafterOneGameTickDelay;
    }

    public static boolean noTntPlaceUpdateEnabled() {
        return noTntPlaceUpdate;
    }

    public static boolean allowPistonDuplicationEnabled() {
        return allowPistonDuplication;
    }

    public static boolean allowPistonDuplicationFor(BlockState state) {
        if (!allowPistonDuplication) {
            return false;
        }
        if (!allowTntDuplication && state.is(Blocks.TNT)) {
            return false;
        }
        if (!allowRailDuplication && state.is(BlockTags.RAILS)) {
            return false;
        }
        return allowCarpetDuplication
                || (!state.is(BlockTags.WOOL_CARPETS) && !state.is(Blocks.MOSS_CARPET) && !state.is(Blocks.PALE_MOSS_CARPET));
    }

    public static boolean allowTntDuplicationEnabled() {
        return allowTntDuplication;
    }

    public static boolean allowRailDuplicationEnabled() {
        return allowRailDuplication;
    }

    public static boolean allowCarpetDuplicationEnabled() {
        return allowCarpetDuplication;
    }

    public static boolean allowGravityBlockEndPortalDuplicationEnabled() {
        return allowGravityBlockEndPortalDuplication;
    }

    public static boolean redstoneIgnoreUpwardsUpdateEnabled() {
        return redstoneIgnoreUpwardsUpdate;
    }

    public static boolean movableBuddingAmethystEnabled() {
        return movableBuddingAmethyst;
    }

    public static boolean stringTripwireHookDuplicateEnabled() {
        return stringTripwireHookDuplicate;
    }

    public static boolean tripwireBehaviorVanilla20() {
        return tripwireBehavior == 20;
    }

    public static boolean tripwireBehaviorVanilla21() {
        return tripwireBehavior == 21;
    }

    public static boolean tripwireBehaviorMixed() {
        return tripwireBehavior == 0;
    }

    public static EventBus events() {
        var runtime = activeRuntime();
        return runtime == null ? NOOP_EVENTS : runtime.events();
    }

    public static ServerPerformance performance() {
        var runtime = activeRuntime();
        return runtime == null ? EMPTY_PERFORMANCE : runtime.performance();
    }

    public static void recordTickPerformance(long tickStartNanos, long tickDurationNanos, long taskExecutionNanos) {
        var runtime = activeRuntime();
        if (runtime != null) {
            runtime.recordTick(tickStartNanos, tickDurationNanos, taskExecutionNanos);
        }
    }

    public static boolean submitChunkTrackingDiff(ServerLevel level, ChunkTrackingSnapshot snapshot) {
        var runtime = activeRuntime();
        return runtime != null
                && runtime.chunkSendScheduler().submitTrackingDiff(level.dimension(), snapshot);
    }

    public static int applyChunkTrackingDiffs(ServerLevel level, ChunkSendScheduler.TrackingDiffApplier applier) {
        var runtime = activeRuntime();
        return runtime == null ? 0 : runtime.chunkSendScheduler().applyCompleted(level.dimension(), applier);
    }

    public static boolean hasListeners(Class<? extends Event> type) {
        return events().hasListeners(type);
    }

    /**
     * Fires {@code event} and logs (but does not propagate) listener failures.
     * Returns {@code event} so callers can chain {@code .cancelled()} reads.
     */
    public static <E extends Event> E fireOrLog(E event, String description) {
        return fireOrLog(events(), event, description);
    }

    public static <E extends Event> E fireOrLog(EventBus bus, E event, String description) {
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("{} listener failed", description, failure);
        }
        return event;
    }

    public static PlayerRegistry players() {
        return Main.runtime().playerRegistry();
    }

    public static @Nullable PlayerRegistry playersOrNull() {
        var runtime = Main.runtimeOrNull();
        return runtime == null ? null : runtime.playerRegistry();
    }

    public static @Nullable FandPlayer findPlayer(UUID id) {
        var runtime = Main.runtimeOrNull();
        return runtime == null ? null : runtime.playerRegistry().findOrNull(id);
    }

    public static Optional<WorldRegistry> worlds() {
        var runtime = Main.runtimeOrNull();
        return runtime == null ? Optional.empty() : Optional.ofNullable(runtime.worldRegistryOrNull());
    }

    public static @Nullable FandWorld wrapWorld(ServerLevel level) {
        var runtime = Main.runtimeOrNull();
        var registry = runtime == null ? null : runtime.worldRegistryOrNull();
        return registry == null ? null : registry.wrap(level);
    }

    public static @Nullable LivingEntity wrapLivingEntity(
            net.minecraft.world.entity.LivingEntity entity
    ) {
        var runtime = Main.runtimeOrNull();
        var registry = runtime == null ? null : runtime.entityRegistryOrNull();
        var wrapped = registry == null ? null : registry.wrap(entity);
        return wrapped instanceof LivingEntity living ? living : null;
    }

    public static @Nullable Entity wrapEntity(net.minecraft.world.entity.Entity entity) {
        var runtime = Main.runtimeOrNull();
        var registry = runtime == null ? null : runtime.entityRegistryOrNull();
        return registry == null ? null : registry.wrap(entity);
    }

    public static net.minecraft.world.entity.@Nullable Entity unwrapEntity(Entity entity) {
        if (entity instanceof FandPlayer player) {
            return player.handle();
        }
        if (entity instanceof FandEntity fandEntity) {
            return fandEntity.handle();
        }
        return null;
    }

    public static Optional<EntityRegistry> entities() {
        var runtime = Main.runtimeOrNull();
        return runtime == null ? Optional.empty() : Optional.ofNullable(runtime.entityRegistryOrNull());
    }

    public static ProxyForwardingMode proxyForwardingMode() {
        var runtime = activeRuntime();
        return runtime == null ? ProxyForwardingMode.NONE : runtime.proxyForwarding().mode();
    }

    public static boolean consoleGuiEnabled() {
        var runtime = activeRuntime();
        return runtime != null && runtime.consoleGuiEnabled();
    }

    public static io.fand.server.console.gui.GuiThemeService guiThemes() {
        var runtime = activeRuntime();
        return runtime == null ? FALLBACK_GUI_THEMES : runtime.guiThemes();
    }

    public static io.fand.server.block.FandCustomBlockRegistry customBlocks() {
        var runtime = activeRuntime();
        return runtime == null ? NOOP_CUSTOM_BLOCKS : runtime.customBlockRegistry();
    }

    public static @Nullable ObjectArrayList<net.minecraft.world.item.ItemStack> generateLootReplacement(
            @Nullable ResourceKey<LootTable> key,
            LootParams params
    ) {
        if (key == null) {
            return null;
        }
        var runtime = activeRuntime();
        if (runtime == null || !(runtime.lootTables() instanceof io.fand.server.loot.FandLootTableService lootTables)) {
            return null;
        }
        try {
            return lootTables.generateVanilla(key, params);
        } catch (RuntimeException failure) {
            LOGGER.warn("Fand loot replacement failed for {}", key.identifier(), failure);
            return null;
        }
    }

    public static ForwardedPlayerInfo parseBungeeLegacyForwarding(String hostName, String playerName) {
        return ProxyForwarding.parseBungeeLegacy(hostName, playerName);
    }

    public static ForwardedPlayerInfo parseVelocityModernForwarding(VelocityForwardingQueryAnswerPayload payload) {
        return ProxyForwarding.parseVelocityModern(Main.runtime().proxyForwarding().secret(), payload);
    }

    public static io.fand.server.auth.FandLoginAuthenticationService.LoginAttempt authenticateLogin(
            io.fand.api.auth.LoginAuthenticationRequest request
    ) {
        var runtime = activeRuntime();
        return runtime == null
                ? io.fand.server.auth.FandLoginAuthenticationService.LoginAttempt.pass()
                : runtime.loginAuthenticators().authenticate(request);
    }

    public static io.fand.server.auth.FandLoginAuthenticationService.LoginAttempt authenticateLoginPlugins(
            io.fand.api.auth.LoginAuthenticationRequest request
    ) {
        var runtime = activeRuntime();
        return runtime == null
                ? io.fand.server.auth.FandLoginAuthenticationService.LoginAttempt.pass()
                : runtime.loginAuthenticators().authenticatePlugins(request);
    }

    public static io.fand.api.nms.NmsHookResult dispatchNmsHook(
            net.kyori.adventure.key.Key hook,
            Object instance,
            Object... arguments
    ) {
        var runtime = activeRuntime();
        if (runtime == null || !(runtime.nms() instanceof io.fand.server.nms.FandNmsService nms)) {
            return io.fand.api.nms.NmsHookResult.pass();
        }
        return nms.dispatch(hook, instance, arguments);
    }

    public static @Nullable Packet<?> interceptInboundPacket(
            PacketListener listener,
            Packet<?> packet,
            @Nullable SocketAddress remoteAddress
    ) {
        var runtime = activeRuntime();
        if (runtime == null) {
            return packet;
        }
        var packetRegistry = runtime.packetRegistry();
        // Per-packet hot path: skip player lookup and bridge dispatch entirely
        // while no plugin has registered any interceptor or custom channel.
        if (!packetRegistry.hasRegistrations()) {
            return packet;
        }
        try {
            return packetRegistry.interceptInbound(listener, player(listener), profile(listener), remoteAddress, packet);
        } catch (RuntimeException failure) {
            LOGGER.warn("Fand inbound packet hook failed for {}", packet.type(), failure);
            return packet;
        }
    }

    public static @Nullable Packet<?> interceptOutboundPacket(
            ConnectionProtocol protocol,
            PacketFlow flow,
            @Nullable PacketListener listener,
            Packet<?> packet,
            @Nullable SocketAddress remoteAddress
    ) {
        var runtime = activeRuntime();
        if (runtime == null) {
            return packet;
        }
        var packetRegistry = runtime.packetRegistry();
        var tabLists = runtime.tabListService();
        if (!packetRegistry.hasRegistrations() && !tabLists.hasPacketOverrides()) {
            return packet;
        }
        try {
            var player = player(listener);
            var rewritten = tabLists.rewriteOutboundPacket(player, packet);
            return packetRegistry.hasRegistrations()
                    ? packetRegistry.interceptOutbound(protocol, flow, player, profile(listener), remoteAddress, rewritten)
                    : rewritten;
        } catch (RuntimeException failure) {
            LOGGER.warn("Fand outbound packet hook failed for {}", packet.type(), failure);
            return packet;
        }
    }

    public static void addPluginChannelConfigurationTask(Queue<ConfigurationTask> tasks) {
        var runtime = activeRuntime();
        if (runtime == null) {
            return;
        }
        try {
            runtime.addPluginChannelConfigurationTask(tasks);
        } catch (RuntimeException failure) {
            LOGGER.warn("Fand plugin channel configuration task failed", failure);
        }
    }

    public static boolean handlePluginChannelConfigurationPayload(
            ServerConfigurationPacketListenerImpl listener,
            Identifier id,
            byte[] payload
    ) {
        var runtime = activeRuntime();
        if (runtime == null) {
            return false;
        }
        try {
            return runtime.handlePluginChannelConfigurationPayload(listener, id, payload);
        } catch (RuntimeException failure) {
            LOGGER.warn("Fand plugin channel configuration payload failed for {}", id, failure);
            return true;
        }
    }

    public static void syncDataPackContents(net.minecraft.server.level.ServerPlayer player, boolean joined) {
        var runtime = activeRuntime();
        if (runtime == null) {
            return;
        }
        try {
            runtime.syncDataPackContents(player, joined);
        } catch (RuntimeException failure) {
            LOGGER.warn("Fand data pack content sync failed for {}", player.getGameProfile().name(), failure);
        }
    }

    private static @Nullable FandServer activeRuntime() {
        var runtime = Main.runtimeOrNull();
        if (runtime == null) {
            return null;
        }
        var phase = runtime.phase();
        return phase == LifecyclePhase.STOPPING || phase == LifecyclePhase.STOPPED ? null : runtime;
    }

    private static ServerPerformance emptyPerformance() {
        return new ServerPerformance(
                emptyWindow(TickWindow.ONE_SECOND),
                emptyWindow(TickWindow.FIVE_SECONDS),
                emptyWindow(TickWindow.TEN_SECONDS),
                emptyWindow(TickWindow.FIFTEEN_SECONDS),
                emptyWindow(TickWindow.ONE_MINUTE),
                emptyWindow(TickWindow.FIVE_MINUTES),
                emptyWindow(TickWindow.FIFTEEN_MINUTES),
                0L,
                0L);
    }

    private static TickWindowSnapshot emptyWindow(TickWindow window) {
        return new TickWindowSnapshot(
                window,
                new MetricStatistics(20.0, 20.0, 20.0, 20.0),
                new MetricStatistics(50.0, 50.0, 50.0, 50.0),
                new MetricStatistics(0.0, 0.0, 0.0, 0.0),
                0.0,
                0);
    }

    private static Optional<? extends Player> player(@Nullable PacketListener listener) {
        if (listener instanceof ServerPlayerConnection connection) {
            return Optional.ofNullable(findPlayer(connection.getPlayer().getUUID()));
        }
        return Optional.empty();
    }

    private static Optional<PlayerProfile> profile(@Nullable PacketListener listener) {
        var player = player(listener);
        if (player.isPresent()) {
            return Optional.of(player.get().profile());
        }
        if (listener instanceof ServerLoginPacketListenerImpl login) {
            return Optional.ofNullable(login.fand$profile())
                    .map(profile -> new PlayerProfile(profile.id(), profile.name()));
        }
        return Optional.empty();
    }
}

