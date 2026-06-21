package io.fand.server.config;

import io.fand.server.console.gui.GuiTheme;
import io.fand.server.console.gui.GuiThemeService;
import io.fand.server.chunk.ChunkSendScheduler;
import io.fand.server.network.ProxyForwardingMode;
import io.fand.server.plugin.PluginRuntime;
import io.fand.server.scheduler.TaskScheduler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diffs an in-memory {@link FandConfig} against the freshly loaded one and
 * tags each changed key as either hot-applied or requires-restart.
 *
 * <p>Centralises the rule "which fields are safe to swap at runtime" so the
 * lifecycle owner ({@code FandServer}) does not grow with each new config key.
 */
public final class ConfigReloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigReloader.class);

    private static final List<ReloadField<FandConfig.Identity, ?>> HOT_IDENTITY_FIELDS = List.of(
            field("identity.brand", (FandConfig.Identity config) -> config.brand)
    );
    private static final List<ReloadField<FandConfig.Plugins, ?>> HOT_PLUGIN_FIELDS = List.of(
            field("plugins.continueOnLoadFailure", (FandConfig.Plugins config) -> config.continueOnLoadFailure),
            field("plugins.continueOnEnableFailure", (FandConfig.Plugins config) -> config.continueOnEnableFailure),
            field("plugins.logSummary", (FandConfig.Plugins config) -> config.logSummary)
    );
    private static final List<ReloadField<FandConfig.Plugins, ?>> RESTART_PLUGIN_FIELDS = List.of(
            field("plugins.directory", (FandConfig.Plugins config) -> config.directory)
    );
    private static final List<ReloadField<FandConfig.Players, ?>> HOT_PLAYER_FIELDS = List.of(
            field("players.speedCheck", (FandConfig.Players config) -> config.speedCheck),
            field("players.logCommands", (FandConfig.Players config) -> config.logCommands)
    );
    private static final List<ReloadField<FandConfig.Chunks, ?>> HOT_CHUNK_FIELDS = List.of(
            field("chunks.workerThreads", (FandConfig.Chunks config) -> config.workerThreads),
            field("chunks.trackingDiffApplyBudget", (FandConfig.Chunks config) -> config.trackingDiffApplyBudget)
    );
    private static final List<ReloadField<FandConfig.Chunks, ?>> RESTART_CHUNK_FIELDS = List.of(
            field("chunks.worldgenParallelism", (FandConfig.Chunks config) -> config.worldgenParallelism),
            field("chunks.dedicatedLightThread", (FandConfig.Chunks config) -> config.dedicatedLightThread),
            field("chunks.lightTaskQueueFastPath", (FandConfig.Chunks config) -> config.lightTaskQueueFastPath)
    );
    private static final List<ReloadField<FandConfig.RecipeViewers, ?>> RESTART_RECIPE_VIEWER_FIELDS = List.of(
            field("compat.modProtocols.recipeViewers.jei", (FandConfig.RecipeViewers config) -> config.jei),
            field("compat.modProtocols.recipeViewers.rei", (FandConfig.RecipeViewers config) -> config.rei)
    );
    private static final List<ReloadField<FandConfig.Performance, ?>> HOT_PERFORMANCE_FIELDS = List.of(
            field("performance.explosionDensityCache", (FandConfig.Performance config) -> config.explosionDensityCache),
            field("performance.collisionTeamCache", (FandConfig.Performance config) -> config.collisionTeamCache),
            field("performance.explosionBlockCache", (FandConfig.Performance config) -> config.explosionBlockCache),
            field("performance.tntDetonationBudget", (FandConfig.Performance config) -> config.tntDetonationBudget),
            field("performance.explosionDropHashMerge",
                    (FandConfig.Performance config) -> config.explosionDropHashMerge),
            field("performance.explosionExposureClipCache",
                    (FandConfig.Performance config) -> config.explosionExposureClipCache),
            field("performance.explosionEntityCache", (FandConfig.Performance config) -> config.explosionEntityCache),
            field("performance.entityHardCollisionCandidateIndex",
                    (FandConfig.Performance config) -> config.entityHardCollisionCandidateIndex),
            field("performance.entitySectionChunkScan",
                    (FandConfig.Performance config) -> config.entitySectionChunkScan),
            field("performance.entityCollisionAbortPropagation",
                    (FandConfig.Performance config) -> config.entityCollisionAbortPropagation),
            field("performance.pushableEntityConsumer",
                    (FandConfig.Performance config) -> config.pushableEntityConsumer),
            field("performance.entityMovementLazyColliders",
                    (FandConfig.Performance config) -> config.entityMovementLazyColliders),
            field("performance.entityTrackerFastPath", (FandConfig.Performance config) -> config.entityTrackerFastPath),
            field("performance.deepPassengerIteration",
                    (FandConfig.Performance config) -> config.deepPassengerIteration),
            field("performance.entityTypeLookupFastPath",
                    (FandConfig.Performance config) -> config.entityTypeLookupFastPath),
            field("performance.randomTickPositionMask",
                    (FandConfig.Performance config) -> config.randomTickPositionMask),
            field("performance.fluidTickContainerQueue",
                    (FandConfig.Performance config) -> config.fluidTickContainerQueue),
            field("performance.flowingFluidSpreadArray",
                    (FandConfig.Performance config) -> config.flowingFluidSpreadArray),
            field("performance.chunkGenerationTaskPlanCache",
                    (FandConfig.Performance config) -> config.chunkGenerationTaskPlanCache),
            field("performance.chunkTaskDispatcherBatchLoop",
                    (FandConfig.Performance config) -> config.chunkTaskDispatcherBatchLoop),
            field("performance.chunkStorageRegionScanFastPath",
                    (FandConfig.Performance config) -> config.chunkStorageRegionScanFastPath),
            field("performance.worldgenSeaLevelCache", (FandConfig.Performance config) -> config.worldgenSeaLevelCache),
            field("performance.fandRedstoneEngine", (FandConfig.Performance config) -> config.fandRedstoneEngine),
            field("performance.itemEntityMergeFastPath",
                    (FandConfig.Performance config) -> config.itemEntityMergeFastPath),
            field("performance.areaEffectCloudFastPath",
                    (FandConfig.Performance config) -> config.areaEffectCloudFastPath),
            field("performance.aiNearestTargetFastPath",
                    (FandConfig.Performance config) -> config.aiNearestTargetFastPath),
            field("performance.aiGoalStreamFastPath",
                    (FandConfig.Performance config) -> config.aiGoalStreamFastPath)
    );
    private static final List<ReloadField<FandConfig.Technical, ?>> HOT_TECHNICAL_FIELDS = List.of(
            field("technical.zeroTickPlants", (FandConfig.Technical config) -> config.zeroTickPlants),
            field("technical.oldHopperSuckInBehavior", (FandConfig.Technical config) -> config.oldHopperSuckInBehavior),
            field("technical.shearsInDispenserCanZeroAmount",
                    (FandConfig.Technical config) -> config.shearsInDispenserCanZeroAmount),
            field("technical.allowEntityPortalWithPassenger",
                    (FandConfig.Technical config) -> config.allowEntityPortalWithPassenger),
            field("technical.disableGatewayPortalEntityTicking",
                    (FandConfig.Technical config) -> config.disableGatewayPortalEntityTicking),
            field("technical.disableLivingEntityAiStepAliveCheck",
                    (FandConfig.Technical config) -> config.disableLivingEntityAiStepAliveCheck),
            field("technical.spawnInvulnerableTime", (FandConfig.Technical config) -> config.spawnInvulnerableTime),
            field("technical.oldZombiePiglinDrop", (FandConfig.Technical config) -> config.oldZombiePiglinDrop),
            field("technical.oldZombieReinforcement", (FandConfig.Technical config) -> config.oldZombieReinforcement),
            field("technical.allowAnvilDestroyItemEntities",
                    (FandConfig.Technical config) -> config.allowAnvilDestroyItemEntities),
            field("technical.disableItemDamageCheck", (FandConfig.Technical config) -> config.disableItemDamageCheck),
            field("technical.keepLeashConnectWhenUseFirework",
                    (FandConfig.Technical config) -> config.keepLeashConnectWhenUseFirework),
            field("technical.tntWetExplosionNoItemDamage",
                    (FandConfig.Technical config) -> config.tntWetExplosionNoItemDamage),
            field("technical.oldProjectileExplosionBehavior",
                    (FandConfig.Technical config) -> config.oldProjectileExplosionBehavior),
            field("technical.oldThrowableProjectileTickOrder",
                    (FandConfig.Technical config) -> config.oldThrowableProjectileTickOrder),
            field("technical.oldMinecartMotionBehavior",
                    (FandConfig.Technical config) -> config.oldMinecartMotionBehavior),
            field("technical.copperBulbOneGameTickDelay",
                    (FandConfig.Technical config) -> config.copperBulbOneGameTickDelay),
            field("technical.crafterOneGameTickDelay",
                    (FandConfig.Technical config) -> config.crafterOneGameTickDelay),
            field("technical.noTntPlaceUpdate", (FandConfig.Technical config) -> config.noTntPlaceUpdate),
            field("technical.allowPistonDuplication", (FandConfig.Technical config) -> config.allowPistonDuplication),
            field("technical.allowTntDuplication", (FandConfig.Technical config) -> config.allowTntDuplication),
            field("technical.allowRailDuplication", (FandConfig.Technical config) -> config.allowRailDuplication),
            field("technical.allowCarpetDuplication", (FandConfig.Technical config) -> config.allowCarpetDuplication),
            field("technical.allowGravityBlockEndPortalDuplication",
                    (FandConfig.Technical config) -> config.allowGravityBlockEndPortalDuplication),
            field("technical.redstoneIgnoreUpwardsUpdate",
                    (FandConfig.Technical config) -> config.redstoneIgnoreUpwardsUpdate),
            field("technical.movableBuddingAmethyst", (FandConfig.Technical config) -> config.movableBuddingAmethyst),
            field("technical.stringTripwireHookDuplicate",
                    (FandConfig.Technical config) -> config.stringTripwireHookDuplicate),
            field("technical.tripwireBehavior", (FandConfig.Technical config) -> config.tripwireBehavior)
    );

    private final Path configPath;
    private final AtomicReference<FandConfig> current;
    private final PluginRuntime plugins;
    private final TaskScheduler scheduler;
    private final ChunkSendScheduler chunks;
    private final GuiThemeService guiThemes;

    public ConfigReloader(
            Path configPath,
            AtomicReference<FandConfig> current,
            PluginRuntime plugins,
            TaskScheduler scheduler,
            ChunkSendScheduler chunks,
            GuiThemeService guiThemes
    ) {
        this.configPath = configPath;
        this.current = current;
        this.plugins = plugins;
        this.scheduler = scheduler;
        this.chunks = chunks;
        this.guiThemes = guiThemes;
    }

    public ConfigReloadResult reload() {
        var previous = current.get();
        var reloaded = FandConfig.load(configPath);
        var changes = new Changes();

        markHot(changes, HOT_IDENTITY_FIELDS, previous.identity, reloaded.identity);
        markHot(changes, HOT_PLUGIN_FIELDS, previous.plugins, reloaded.plugins);
        markRestart(changes, RESTART_PLUGIN_FIELDS, previous.plugins, reloaded.plugins);
        markHot(changes, HOT_PLAYER_FIELDS, previous.players, reloaded.players);
        apply("players config", () -> io.fand.server.hooks.FandHooks.applyPlayerConfig(reloaded.players));
        if (changed(previous.scheduler.asyncThreads, reloaded.scheduler.asyncThreads)) {
            changes.hot("scheduler.asyncThreads");
            apply("scheduler.asyncThreads", () -> scheduler.reconfigureAsyncThreads(reloaded.scheduler.asyncThreads));
        }
        changes.restart("scheduler.regionThreads", previous.scheduler.regionThreads, reloaded.scheduler.regionThreads);
        if (markHot(changes, HOT_CHUNK_FIELDS, previous.chunks, reloaded.chunks)) {
            apply("chunks", () -> chunks.reconfigure(reloaded.chunks));
        }
        markRestart(changes, RESTART_CHUNK_FIELDS, previous.chunks, reloaded.chunks);
        apply("chunks config", () -> io.fand.server.hooks.FandHooks.applyChunkConfig(reloaded.chunks));
        markRestart(
                changes,
                RESTART_RECIPE_VIEWER_FIELDS,
                previous.compat.modProtocols.recipeViewers,
                reloaded.compat.modProtocols.recipeViewers);
        changes.restart(
                "network.forwarding.mode",
                ProxyForwardingMode.fromConfig(previous.network.forwarding.mode),
                ProxyForwardingMode.fromConfig(reloaded.network.forwarding.mode));
        changes.restart(
                "network.forwarding.secret",
                previous.network.forwarding.secret,
                reloaded.network.forwarding.secret);
        var previousTheme = GuiTheme.fromConfig(previous.console.gui.theme);
        var reloadedTheme = GuiTheme.fromConfig(reloaded.console.gui.theme);
        if (changed(previousTheme, reloadedTheme)) {
            changes.hot("console.gui.theme");
            apply("console.gui.theme", () -> guiThemes.set(reloadedTheme));
        }
        changes.restart("console.gui.enabled", previous.console.gui.enabled, reloaded.console.gui.enabled);
        markHot(changes, HOT_PERFORMANCE_FIELDS, previous.performance, reloaded.performance);
        apply("performance config", () -> io.fand.server.hooks.FandHooks.applyPerformanceConfig(reloaded.performance));
        markHot(changes, HOT_TECHNICAL_FIELDS, previous.technical, reloaded.technical);
        apply("technical config", () -> io.fand.server.hooks.FandHooks.applyTechnicalConfig(reloaded.technical));

        apply("plugins", () -> plugins.reconfigure(toPluginOptions(reloaded)));
        // Publish the new baseline unconditionally: subsystems already mutated to the
        // new values, so keeping `previous` here would desync the next reload's diff.
        current.set(reloaded);
        return changes.result();
    }

    private static void apply(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            LOGGER.warn("Failed to apply {} during config reload", what, failure);
        }
    }

    public static PluginRuntime.Options toPluginOptions(FandConfig config) {
        return new PluginRuntime.Options(
                config.plugins.continueOnLoadFailure,
                config.plugins.continueOnEnableFailure,
                config.plugins.logSummary
        );
    }

    private static <C, V> ReloadField<C, V> field(String key, Function<C, V> value) {
        return new ReloadField<>(key, value);
    }

    private static <C> boolean markHot(
            Changes changes,
            List<ReloadField<C, ?>> fields,
            C previous,
            C reloaded
    ) {
        var changed = false;
        for (var field : fields) {
            changed |= field.hot(changes, previous, reloaded);
        }
        return changed;
    }

    private static <C> void markRestart(
            Changes changes,
            List<ReloadField<C, ?>> fields,
            C previous,
            C reloaded
    ) {
        for (var field : fields) {
            field.restart(changes, previous, reloaded);
        }
    }

    private static boolean changed(Object previous, Object reloaded) {
        return !Objects.equals(previous, reloaded);
    }

    private record ReloadField<C, V>(String key, Function<C, V> value) {

        private boolean hot(Changes changes, C previous, C reloaded) {
            return changes.hot(key, value.apply(previous), value.apply(reloaded));
        }

        private void restart(Changes changes, C previous, C reloaded) {
            changes.restart(key, value.apply(previous), value.apply(reloaded));
        }
    }

    private static final class Changes {

        private final ArrayList<String> hotApplied = new ArrayList<>();
        private final ArrayList<String> requiresRestart = new ArrayList<>();

        private boolean hot(String key, Object previous, Object reloaded) {
            if (!changed(previous, reloaded)) {
                return false;
            }
            hot(key);
            return true;
        }

        private void hot(String key) {
            hotApplied.add(key);
        }

        private void restart(String key, Object previous, Object reloaded) {
            if (changed(previous, reloaded)) {
                requiresRestart.add(key);
            }
        }

        private ConfigReloadResult result() {
            return new ConfigReloadResult(hotApplied, requiresRestart);
        }
    }
}
