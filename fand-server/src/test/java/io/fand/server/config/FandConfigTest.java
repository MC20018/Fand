package io.fand.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void writesDefaultsWhenFileIsMissing() throws Exception {
        var path = tempDir.resolve("fand.yml");

        var config = FandConfig.load(path);

        assertThat(config.identity.brand).isEqualTo("Fand");
        assertThat(config.plugins.directory).isEqualTo("plugins");
        assertThat(config.plugins.continueOnLoadFailure).isTrue();
        assertThat(config.plugins.continueOnEnableFailure).isTrue();
        assertThat(config.plugins.logSummary).isTrue();
        assertThat(config.players.speedCheck).isTrue();
        assertThat(config.players.logCommands).isTrue();
        assertThat(config.scheduler.asyncThreads).isZero();
        assertThat(config.scheduler.regionThreads).isZero();
        assertThat(config.chunks.backgroundThreads).isZero();
        assertThat(config.chunks.worldgenThreads).isZero();
        assertThat(config.chunks.workerThreads).isZero();
        assertThat(config.chunks.trackingDiffApplyBudget).isEqualTo(256);
        assertThat(config.chunks.asyncChunkPacketPreparation).isFalse();
        assertThat(config.chunks.worldgenParallelism).isZero();
        assertThat(config.chunks.dedicatedLightThread).isTrue();
        assertThat(config.chunks.lightTaskQueueFastPath).isTrue();
        assertThat(config.chunks.teleportPreload).isTrue();
        assertThat(config.chunks.teleportPreloadExtraRadius).isEqualTo(3);
        assertThat(config.chunks.teleportPreloadSimulation).isTrue();
        assertThat(config.chunks.teleportChunkSendBurstTicks).isEqualTo(40);
        assertThat(config.chunks.teleportChunkSendBurstChunksPerTick).isEqualTo(64);
        assertThat(config.chunks.teleportChunkSendBurstBatches).isEqualTo(10);
        assertThat(config.console.gui.enabled).isTrue();
        assertThat(config.console.gui.theme).isEqualTo("system");
        assertThat(config.network.forwarding.mode).isEqualTo("none");
        assertThat(config.network.forwarding.secret).isEmpty();
        assertThat(config.compat.modProtocols.recipeViewers.jei).isTrue();
        assertThat(config.compat.modProtocols.recipeViewers.rei).isTrue();
        assertThat(config.compat.modProtocols.servux.enabled).isTrue();
        assertThat(config.compat.modProtocols.servux.hud).isTrue();
        assertThat(config.compat.modProtocols.servux.shareWeather).isTrue();
        assertThat(config.compat.modProtocols.servux.entityData).isTrue();
        assertThat(config.compat.modProtocols.servux.structures).isTrue();
        assertThat(config.compat.modProtocols.servux.structureWhitelistEnabled).isFalse();
        assertThat(config.compat.modProtocols.servux.structureBlacklistEnabled).isTrue();
        assertThat(config.compat.modProtocols.servux.litematica).isTrue();
        assertThat(config.compat.modProtocols.servux.litematicaPaste).isFalse();
        assertThat(config.compat.modProtocols.servux.tweaks).isTrue();
        assertThat(config.performance.entityHardCollisionCandidateIndex).isTrue();
        assertThat(config.performance.entitySectionChunkScan).isTrue();
        assertThat(config.performance.entityCollisionAbortPropagation).isTrue();
        assertThat(config.performance.pushableEntityConsumer).isTrue();
        assertThat(config.performance.entityMovementLazyColliders).isTrue();
        assertThat(config.performance.entityTrackerFastPath).isTrue();
        assertThat(config.performance.deepPassengerIteration).isTrue();
        assertThat(config.performance.entityTypeLookupFastPath).isTrue();
        assertThat(config.performance.randomTickPositionMask).isTrue();
        assertThat(config.performance.chunkGenerationTaskPlanCache).isTrue();
        assertThat(config.performance.chunkTaskDispatcherBatchLoop).isTrue();
        assertThat(config.performance.chunkStorageRegionScanFastPath).isTrue();
        assertThat(config.performance.worldgenSeaLevelCache).isTrue();
        assertThat(config.performance.playerNameLookupIndex).isTrue();
        assertThat(config.performance.scoreboardTeamWaypointFastPath).isTrue();
        assertThat(config.performance.reusablePacketEncoding).isTrue();
        assertThat(config.performance.packetFlushCoalescing).isTrue();
        assertThat(config.performance.outboundPacketQueueCoalescing).isTrue();
        assertThat(config.performance.aiSensorLoopFastPath).isTrue();
        assertThat(config.technical.zeroTickPlants).isFalse();
        assertThat(config.technical.oldHopperSuckInBehavior).isFalse();
        assertThat(config.technical.shearsInDispenserCanZeroAmount).isFalse();
        assertThat(config.technical.allowEntityPortalWithPassenger).isTrue();
        assertThat(config.technical.disableGatewayPortalEntityTicking).isFalse();
        assertThat(config.technical.disableLivingEntityAiStepAliveCheck).isFalse();
        assertThat(config.technical.spawnInvulnerableTime).isFalse();
        assertThat(config.technical.oldZombiePiglinDrop).isFalse();
        assertThat(config.technical.oldZombieReinforcement).isFalse();
        assertThat(config.technical.allowAnvilDestroyItemEntities).isFalse();
        assertThat(config.technical.disableItemDamageCheck).isFalse();
        assertThat(config.technical.keepLeashConnectWhenUseFirework).isFalse();
        assertThat(config.technical.tntWetExplosionNoItemDamage).isFalse();
        assertThat(config.technical.oldProjectileExplosionBehavior).isFalse();
        assertThat(config.technical.oldThrowableProjectileTickOrder).isFalse();
        assertThat(config.technical.oldMinecartMotionBehavior).isFalse();
        assertThat(config.technical.copperBulbOneGameTickDelay).isFalse();
        assertThat(config.technical.crafterOneGameTickDelay).isFalse();
        assertThat(config.technical.noTntPlaceUpdate).isFalse();
        assertThat(config.technical.allowPistonDuplication).isTrue();
        assertThat(config.technical.allowTntDuplication).isTrue();
        assertThat(config.technical.allowRailDuplication).isTrue();
        assertThat(config.technical.allowCarpetDuplication).isTrue();
        assertThat(config.technical.allowGravityBlockEndPortalDuplication).isTrue();
        assertThat(config.technical.redstoneIgnoreUpwardsUpdate).isFalse();
        assertThat(config.technical.movableBuddingAmethyst).isFalse();
        assertThat(config.technical.stringTripwireHookDuplicate).isTrue();
        assertThat(config.technical.tripwireBehavior).isEqualTo("vanilla_21");
        assertThat(Files.readString(path))
                .contains("# Public-facing identity settings.")
                .contains("identity:")
                .contains("brand: Fand")
                .contains("plugins:")
                .contains("directory: plugins")
                .contains("continueOnLoadFailure: true")
                .contains("continueOnEnableFailure: true")
                .contains("logSummary: true")
                .contains("players:")
                .contains("speedCheck: true")
                .contains("logCommands: true")
                .contains("scheduler:")
                .contains("asyncThreads: 0")
                .contains("regionThreads: 0")
                .contains("chunks:")
                .contains("backgroundThreads: 0")
                .contains("worldgenThreads: 0")
                .contains("workerThreads: 0")
                .contains("trackingDiffApplyBudget: 256")
                .contains("asyncChunkPacketPreparation: false")
                .contains("worldgenParallelism: 0")
                .contains("dedicatedLightThread: true")
                .contains("lightTaskQueueFastPath: true")
                .contains("teleportPreload: true")
                .contains("teleportPreloadExtraRadius: 3")
                .contains("teleportPreloadSimulation: true")
                .contains("teleportChunkSendBurstTicks: 40")
                .contains("teleportChunkSendBurstChunksPerTick: 64")
                .contains("teleportChunkSendBurstBatches: 10")
                .contains("console:")
                .contains("gui:")
                .contains("enabled: true")
                .contains("theme: system")
                .contains("network:")
                .contains("forwarding:")
                .contains("mode: none")
                .contains("secret: ''")
                .contains("compat:")
                .contains("modProtocols:")
                .contains("recipeViewers:")
                .contains("jei: true")
                .contains("rei: true")
                .contains("servux:")
                .contains("enabled: true")
                .contains("hud: true")
                .contains("entityData: true")
                .contains("structures: true")
                .contains("litematica: true")
                .contains("litematicaPaste: false")
                .contains("tweaks: true")
                .contains("performance:")
                .contains("entityHardCollisionCandidateIndex: true")
                .contains("entitySectionChunkScan: true")
                .contains("entityCollisionAbortPropagation: true")
                .contains("pushableEntityConsumer: true")
                .contains("entityMovementLazyColliders: true")
                .contains("entityTrackerFastPath: true")
                .contains("deepPassengerIteration: true")
                .contains("entityTypeLookupFastPath: true")
                .contains("randomTickPositionMask: true")
                .contains("chunkGenerationTaskPlanCache: true")
                .contains("chunkTaskDispatcherBatchLoop: true")
                .contains("chunkStorageRegionScanFastPath: true")
                .contains("worldgenSeaLevelCache: true")
                .contains("playerNameLookupIndex: true")
                .contains("scoreboardTeamWaypointFastPath: true")
                .contains("reusablePacketEncoding: true")
                .contains("packetFlushCoalescing: true")
                .contains("outboundPacketQueueCoalescing: true")
                .contains("aiSensorLoopFastPath: true")
                .contains("technical:")
                .contains("zeroTickPlants: false")
                .contains("oldHopperSuckInBehavior: false")
                .contains("shearsInDispenserCanZeroAmount: false")
                .contains("allowEntityPortalWithPassenger: true")
                .contains("disableGatewayPortalEntityTicking: false")
                .contains("disableLivingEntityAiStepAliveCheck: false")
                .contains("spawnInvulnerableTime: false")
                .contains("oldZombiePiglinDrop: false")
                .contains("oldZombieReinforcement: false")
                .contains("allowAnvilDestroyItemEntities: false")
                .contains("disableItemDamageCheck: false")
                .contains("keepLeashConnectWhenUseFirework: false")
                .contains("tntWetExplosionNoItemDamage: false")
                .contains("oldProjectileExplosionBehavior: false")
                .contains("oldThrowableProjectileTickOrder: false")
                .contains("oldMinecartMotionBehavior: false")
                .contains("copperBulbOneGameTickDelay: false")
                .contains("crafterOneGameTickDelay: false")
                .contains("noTntPlaceUpdate: false")
                .contains("allowPistonDuplication: true")
                .contains("allowTntDuplication: true")
                .contains("allowRailDuplication: true")
                .contains("allowCarpetDuplication: true")
                .contains("allowGravityBlockEndPortalDuplication: true")
                .contains("redstoneIgnoreUpwardsUpdate: false")
                .contains("movableBuddingAmethyst: false")
                .contains("stringTripwireHookDuplicate: true")
                .contains("tripwireBehavior: vanilla_21");
    }

    @Test
    void loadsConfiguredValues() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                identity:
                  brand: 'My Fand'

                plugins:
                  directory: 'mods/plugins'
                  continueOnLoadFailure: true
                  continueOnEnableFailure: true
                  logSummary: false

                players:
                  speedCheck: false
                  logCommands: false

                scheduler:
                  asyncThreads: 6
                  regionThreads: 4

                chunks:
                  backgroundThreads: 5
                  worldgenThreads: 4
                  workerThreads: 3
                  trackingDiffApplyBudget: 64
                  asyncChunkPacketPreparation: true
                  worldgenParallelism: 6
                  dedicatedLightThread: false
                  lightTaskQueueFastPath: false
                  teleportPreload: false
                  teleportPreloadExtraRadius: 7
                  teleportPreloadSimulation: false
                  teleportChunkSendBurstTicks: 12
                  teleportChunkSendBurstChunksPerTick: 128
                  teleportChunkSendBurstBatches: 6

                console:
                  gui:
                    enabled: false
                    theme: dark

                network:
                  forwarding:
                    mode: velocity-modern
                    secret: 'shared-secret'

                compat:
                  modProtocols:
                    recipeViewers:
                      jei: false
                      rei: false
                    servux:
                      enabled: false
                      hud: false
                      hudPermissionLevel: 1
                      hudUpdateIntervalTicks: 60
                      hudLoggers: false
                      hudLoggerPermissionLevel: 1
                      shareWeather: false
                      weatherPermissionLevel: 2
                      shareSeed: true
                      seedPermissionLevel: 3
                      entityData: false
                      entityPermissionLevel: 1
                      playerInventory: true
                      playerInventoryPermissionLevel: 3
                      playerEnderItems: true
                      playerEnderItemsPermissionLevel: 3
                      structures: false
                      structuresPermissionLevel: 1
                      structuresUpdateIntervalTicks: 80
                      structuresTimeoutTicks: 1200
                      structureWhitelistEnabled: true
                      structureWhitelist: 'minecraft:village'
                      structureBlacklistEnabled: false
                      structureBlacklist: ''
                      litematica: false
                      litematicaPermissionLevel: 1
                      litematicaPaste: true
                      litematicaPastePermissionLevel: 3
                      tweaks: false
                      tweaksPermissionLevel: 1
                      stackableShulkers: true
                      stackableShulkerSize: 16

                performance:
                  entityHardCollisionCandidateIndex: false
                  entitySectionChunkScan: false
                  entityCollisionAbortPropagation: false
                  pushableEntityConsumer: false
                  entityMovementLazyColliders: false
                  entityTrackerFastPath: false
                  deepPassengerIteration: false
                  entityTypeLookupFastPath: false
                  randomTickPositionMask: false
                  chunkGenerationTaskPlanCache: false
                  chunkTaskDispatcherBatchLoop: false
                  chunkStorageRegionScanFastPath: false
                  worldgenSeaLevelCache: false
                  playerNameLookupIndex: false
                  scoreboardTeamWaypointFastPath: false
                  reusablePacketEncoding: false
                  packetFlushCoalescing: false
                  outboundPacketQueueCoalescing: false
                  aiSensorLoopFastPath: false

                technical:
                  zeroTickPlants: true
                  oldHopperSuckInBehavior: true
                  shearsInDispenserCanZeroAmount: true
                  allowEntityPortalWithPassenger: false
                  disableGatewayPortalEntityTicking: true
                  disableLivingEntityAiStepAliveCheck: true
                  spawnInvulnerableTime: true
                  oldZombiePiglinDrop: true
                  oldZombieReinforcement: true
                  allowAnvilDestroyItemEntities: true
                  disableItemDamageCheck: true
                  keepLeashConnectWhenUseFirework: true
                  tntWetExplosionNoItemDamage: true
                  oldProjectileExplosionBehavior: true
                  oldThrowableProjectileTickOrder: true
                  oldMinecartMotionBehavior: true
                  copperBulbOneGameTickDelay: true
                  crafterOneGameTickDelay: true
                  noTntPlaceUpdate: true
                  allowPistonDuplication: false
                  allowTntDuplication: false
                  allowRailDuplication: false
                  allowCarpetDuplication: false
                  allowGravityBlockEndPortalDuplication: false
                  redstoneIgnoreUpwardsUpdate: true
                  movableBuddingAmethyst: true
                  stringTripwireHookDuplicate: true
                  tripwireBehavior: mixed
                """);

        var config = FandConfig.load(path);

        assertThat(config.identity.brand).isEqualTo("My Fand");
        assertThat(config.plugins.directory).isEqualTo("mods/plugins");
        assertThat(config.plugins.continueOnLoadFailure).isTrue();
        assertThat(config.plugins.continueOnEnableFailure).isTrue();
        assertThat(config.plugins.logSummary).isFalse();
        assertThat(config.players.speedCheck).isFalse();
        assertThat(config.players.logCommands).isFalse();
        assertThat(config.scheduler.asyncThreads).isEqualTo(6);
        assertThat(config.scheduler.regionThreads).isEqualTo(4);
        assertThat(config.chunks.backgroundThreads).isEqualTo(5);
        assertThat(config.chunks.worldgenThreads).isEqualTo(4);
        assertThat(config.chunks.workerThreads).isEqualTo(3);
        assertThat(config.chunks.trackingDiffApplyBudget).isEqualTo(64);
        assertThat(config.chunks.asyncChunkPacketPreparation).isTrue();
        assertThat(config.chunks.worldgenParallelism).isEqualTo(6);
        assertThat(config.chunks.dedicatedLightThread).isFalse();
        assertThat(config.chunks.lightTaskQueueFastPath).isFalse();
        assertThat(config.chunks.teleportPreload).isFalse();
        assertThat(config.chunks.teleportPreloadExtraRadius).isEqualTo(7);
        assertThat(config.chunks.teleportPreloadSimulation).isFalse();
        assertThat(config.chunks.teleportChunkSendBurstTicks).isEqualTo(12);
        assertThat(config.chunks.teleportChunkSendBurstChunksPerTick).isEqualTo(128);
        assertThat(config.chunks.teleportChunkSendBurstBatches).isEqualTo(6);
        assertThat(config.console.gui.enabled).isFalse();
        assertThat(config.console.gui.theme).isEqualTo("dark");
        assertThat(config.network.forwarding.mode).isEqualTo("velocity-modern");
        assertThat(config.network.forwarding.secret).isEqualTo("shared-secret");
        assertThat(config.compat.modProtocols.recipeViewers.jei).isFalse();
        assertThat(config.compat.modProtocols.recipeViewers.rei).isFalse();
        assertThat(config.compat.modProtocols.servux.enabled).isFalse();
        assertThat(config.compat.modProtocols.servux.hud).isFalse();
        assertThat(config.compat.modProtocols.servux.hudPermissionLevel).isEqualTo(1);
        assertThat(config.compat.modProtocols.servux.hudUpdateIntervalTicks).isEqualTo(60);
        assertThat(config.compat.modProtocols.servux.hudLoggers).isFalse();
        assertThat(config.compat.modProtocols.servux.hudLoggerPermissionLevel).isEqualTo(1);
        assertThat(config.compat.modProtocols.servux.shareWeather).isFalse();
        assertThat(config.compat.modProtocols.servux.weatherPermissionLevel).isEqualTo(2);
        assertThat(config.compat.modProtocols.servux.shareSeed).isTrue();
        assertThat(config.compat.modProtocols.servux.seedPermissionLevel).isEqualTo(3);
        assertThat(config.compat.modProtocols.servux.entityData).isFalse();
        assertThat(config.compat.modProtocols.servux.entityPermissionLevel).isEqualTo(1);
        assertThat(config.compat.modProtocols.servux.playerInventory).isTrue();
        assertThat(config.compat.modProtocols.servux.playerInventoryPermissionLevel).isEqualTo(3);
        assertThat(config.compat.modProtocols.servux.playerEnderItems).isTrue();
        assertThat(config.compat.modProtocols.servux.playerEnderItemsPermissionLevel).isEqualTo(3);
        assertThat(config.compat.modProtocols.servux.structures).isFalse();
        assertThat(config.compat.modProtocols.servux.structuresPermissionLevel).isEqualTo(1);
        assertThat(config.compat.modProtocols.servux.structuresUpdateIntervalTicks).isEqualTo(80);
        assertThat(config.compat.modProtocols.servux.structuresTimeoutTicks).isEqualTo(1200);
        assertThat(config.compat.modProtocols.servux.structureWhitelistEnabled).isTrue();
        assertThat(config.compat.modProtocols.servux.structureWhitelist).isEqualTo("minecraft:village");
        assertThat(config.compat.modProtocols.servux.structureBlacklistEnabled).isFalse();
        assertThat(config.compat.modProtocols.servux.structureBlacklist).isEmpty();
        assertThat(config.compat.modProtocols.servux.litematica).isFalse();
        assertThat(config.compat.modProtocols.servux.litematicaPermissionLevel).isEqualTo(1);
        assertThat(config.compat.modProtocols.servux.litematicaPaste).isTrue();
        assertThat(config.compat.modProtocols.servux.litematicaPastePermissionLevel).isEqualTo(3);
        assertThat(config.compat.modProtocols.servux.tweaks).isFalse();
        assertThat(config.compat.modProtocols.servux.tweaksPermissionLevel).isEqualTo(1);
        assertThat(config.compat.modProtocols.servux.stackableShulkers).isTrue();
        assertThat(config.compat.modProtocols.servux.stackableShulkerSize).isEqualTo(16);
        assertThat(config.performance.entityHardCollisionCandidateIndex).isFalse();
        assertThat(config.performance.entitySectionChunkScan).isFalse();
        assertThat(config.performance.entityCollisionAbortPropagation).isFalse();
        assertThat(config.performance.pushableEntityConsumer).isFalse();
        assertThat(config.performance.entityMovementLazyColliders).isFalse();
        assertThat(config.performance.entityTrackerFastPath).isFalse();
        assertThat(config.performance.deepPassengerIteration).isFalse();
        assertThat(config.performance.entityTypeLookupFastPath).isFalse();
        assertThat(config.performance.randomTickPositionMask).isFalse();
        assertThat(config.performance.chunkGenerationTaskPlanCache).isFalse();
        assertThat(config.performance.chunkTaskDispatcherBatchLoop).isFalse();
        assertThat(config.performance.chunkStorageRegionScanFastPath).isFalse();
        assertThat(config.performance.worldgenSeaLevelCache).isFalse();
        assertThat(config.performance.playerNameLookupIndex).isFalse();
        assertThat(config.performance.scoreboardTeamWaypointFastPath).isFalse();
        assertThat(config.performance.reusablePacketEncoding).isFalse();
        assertThat(config.performance.packetFlushCoalescing).isFalse();
        assertThat(config.performance.outboundPacketQueueCoalescing).isFalse();
        assertThat(config.performance.aiSensorLoopFastPath).isFalse();
        assertThat(config.technical.zeroTickPlants).isTrue();
        assertThat(config.technical.oldHopperSuckInBehavior).isTrue();
        assertThat(config.technical.shearsInDispenserCanZeroAmount).isTrue();
        assertThat(config.technical.allowEntityPortalWithPassenger).isFalse();
        assertThat(config.technical.disableGatewayPortalEntityTicking).isTrue();
        assertThat(config.technical.disableLivingEntityAiStepAliveCheck).isTrue();
        assertThat(config.technical.spawnInvulnerableTime).isTrue();
        assertThat(config.technical.oldZombiePiglinDrop).isTrue();
        assertThat(config.technical.oldZombieReinforcement).isTrue();
        assertThat(config.technical.allowAnvilDestroyItemEntities).isTrue();
        assertThat(config.technical.disableItemDamageCheck).isTrue();
        assertThat(config.technical.keepLeashConnectWhenUseFirework).isTrue();
        assertThat(config.technical.tntWetExplosionNoItemDamage).isTrue();
        assertThat(config.technical.oldProjectileExplosionBehavior).isTrue();
        assertThat(config.technical.oldThrowableProjectileTickOrder).isTrue();
        assertThat(config.technical.oldMinecartMotionBehavior).isTrue();
        assertThat(config.technical.copperBulbOneGameTickDelay).isTrue();
        assertThat(config.technical.crafterOneGameTickDelay).isTrue();
        assertThat(config.technical.noTntPlaceUpdate).isTrue();
        assertThat(config.technical.allowPistonDuplication).isFalse();
        assertThat(config.technical.allowTntDuplication).isFalse();
        assertThat(config.technical.allowRailDuplication).isFalse();
        assertThat(config.technical.allowCarpetDuplication).isFalse();
        assertThat(config.technical.allowGravityBlockEndPortalDuplication).isFalse();
        assertThat(config.technical.redstoneIgnoreUpwardsUpdate).isTrue();
        assertThat(config.technical.movableBuddingAmethyst).isTrue();
        assertThat(config.technical.stringTripwireHookDuplicate).isTrue();
        assertThat(config.technical.tripwireBehavior).isEqualTo("mixed");
    }

    @Test
    void acceptsBungeeForwardingAlias() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                network:
                  forwarding:
                    mode: bc
                    secret: ''
                """);

        var config = FandConfig.load(path);

        assertThat(config.network.forwarding.mode).isEqualTo("bc");
    }

    @Test
    void rejectsOutOfRangeValues() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                scheduler:
                  asyncThreads: -1
                """);

        assertThatThrownBy(() -> FandConfig.load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("scheduler.asyncThreads");
    }

    @Test
    void rejectsOutOfRangeRegionThreads() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                scheduler:
                  regionThreads: -1
                """);

        assertThatThrownBy(() -> FandConfig.load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("scheduler.regionThreads");
    }

    @Test
    void rejectsVelocityForwardingWithoutSecret() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                network:
                  forwarding:
                    mode: velocity-modern
                """);

        assertThatThrownBy(() -> FandConfig.load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("network.forwarding.secret");
    }

    @Test
    void rejectsUnknownTripwireBehavior() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                technical:
                  tripwireBehavior: classic
                """);

        assertThatThrownBy(() -> FandConfig.load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("technical.tripwireBehavior");
    }
}

