package io.fand.server.config;

import io.fand.server.network.ProxyForwardingMode;
import java.nio.file.Path;

public final class FandConfig {

    @ConfigComment("Public-facing identity settings.")
    public final Identity identity = new Identity();

    @ConfigComment("Plugin runtime settings.")
    public final Plugins plugins = new Plugins();

    @ConfigComment("Player safety and audit settings.")
    public final Players players = new Players();

    @ConfigComment("Async scheduler settings.")
    public final Scheduler scheduler = new Scheduler();

    @ConfigComment("Server console and GUI settings.")
    public final Console console = new Console();

    @ConfigComment("Network and proxy settings.")
    public final Network network = new Network();

    @ConfigComment("Compatibility shims for mod-side protocols.")
    public final Compat compat = new Compat();

    @ConfigComment("Chunk loading and player chunk-send scheduling settings.")
    public final Chunks chunks = new Chunks();

    @ConfigComment("Game-path performance optimizations. All entries preserve vanilla behaviour unless noted.")
    public final Performance performance = new Performance();

    @ConfigComment({
            "Technical survival and legacy-mechanic toggles.",
            "Defaults prefer technical-survival compatibility for explicitly allowed legacy mechanics."
    })
    public final Technical technical = new Technical();

    public static FandConfig load(Path path) {
        var config = new YamlConfigLoader<>(FandConfig.class).load(path);
        validate(config);
        return config;
    }

    public static void save(Path path, FandConfig config) {
        validate(config);
        new YamlConfigLoader<>(FandConfig.class).save(path, config);
    }

    private static void validate(FandConfig config) {
        var mode = ProxyForwardingMode.fromConfig(config.network.forwarding.mode);
        if (mode.requiresSecret() && config.network.forwarding.secret.isBlank()) {
            throw new ConfigException("network.forwarding.secret must be set when network.forwarding.mode is " + mode.configValue());
        }
        io.fand.server.console.gui.GuiTheme.fromConfig(config.console.gui.theme);
        validateTripwireBehavior(config.technical.tripwireBehavior);
    }

    private static void validateTripwireBehavior(String value) {
        switch (value) {
            case "vanilla_21", "vanilla_20", "mixed" -> {
            }
            default -> throw new ConfigException(
                    "technical.tripwireBehavior must be one of: vanilla_21, vanilla_20, mixed");
        }
    }

    public static final class Identity {

        @ConfigComment("Brand reported by the server and plugin API.")
        public String brand = "Fand";
    }

    public static final class Plugins {

        @ConfigComment("Directory scanned for plugin jars and plugin data folders.")
        public String directory = "plugins";

        @ConfigComment("Continue boot if a plugin fails during discovery, construction, or onLoad.")
        public boolean continueOnLoadFailure = true;

        @ConfigComment("Continue boot if a plugin fails during onEnable.")
        public boolean continueOnEnableFailure = true;

        @ConfigComment("Log a summary after plugin load and enable phases.")
        public boolean logSummary = true;
    }

    public static final class Players {

        @ConfigComment({
                "Keep vanilla's player movement speed check enabled.",
                "When disabled, Fand skips the moved-too-quickly warning and",
                "rubber-band correction; collision and invalid movement checks",
                "still run."
        })
        public volatile boolean speedCheck = true;

        @ConfigComment("Log player-entered commands to the server console.")
        public volatile boolean logCommands = true;
    }

    public static final class Scheduler {

        @ConfigComment({
                "Number of async scheduler threads.",
                "Set to 0 to derive the value from available processors."
        })
        @ConfigRange(min = 0, max = 1024)
        public int asyncThreads = 0;

        @ConfigComment({
                "Number of region scheduler worker lanes.",
                "Tasks in the same 8x8 chunk region run serially; different",
                "regions may run in parallel. Set to 0 to derive the value",
                "from available processors. Existing schedulers need a restart",
                "to replace their region lanes."
        })
        @ConfigRange(min = 0, max = 1024)
        public int regionThreads = 0;
    }

    public static final class Compat {

        @ConfigComment("Mod protocol compatibility settings.")
        public final ModProtocols modProtocols = new ModProtocols();
    }

    public static final class ModProtocols {

        @ConfigComment("Recipe viewer protocol compatibility settings.")
        public final RecipeViewers recipeViewers = new RecipeViewers();
    }

    public static final class RecipeViewers {

        @ConfigComment("Expose JEI-compatible plugin messaging channels.")
        public volatile boolean jei = true;

        @ConfigComment("Expose REI-compatible plugin messaging channels.")
        public volatile boolean rei = true;
    }

    public static final class Chunks {

        @ConfigComment({
                "Worker threads used for safe chunk tracking diff calculations.",
                "Set to 0 to derive the value from available processors."
        })
        @ConfigRange(min = 0, max = 64)
        public int workerThreads = 0;

        @ConfigComment({
                "Maximum player chunk tracking diff jobs completed per server tick.",
                "Set to 0 to apply every completed job in the same tick."
        })
        @ConfigRange(min = 0, max = 4096)
        public int trackingDiffApplyBudget = 256;

        @ConfigComment({
                "Maximum chunk-generation batches allowed to run at the same",
                "time. Set to 1 for vanilla's serialized worldgen dispatcher,",
                "or 0 to derive a conservative value from available processors.",
                "Values above 1 run on dedicated Fand worldgen worker lanes.",
                "Parallel batches are limited to non-overlapping block-write",
                "envelopes, preserving vanilla chunk status dependencies and",
                "generated content. Existing worlds need a restart to replace",
                "their dispatcher."
        })
        @ConfigRange(min = 0, max = 64)
        public int worldgenParallelism = 0;

        @ConfigComment({
                "Run chunk lighting on one dedicated worker thread per world",
                "instead of the shared chunk executor. Lighting tasks remain",
                "serialized in the same light dispatcher; this only prevents",
                "lighting from competing with worldgen and other chunk workers.",
                "Existing worlds need a restart to replace their light executor."
        })
        public boolean dedicatedLightThread = true;

        @ConfigComment({
                "Store queued chunk-light tasks in parallel arrays instead of",
                "allocating one Pair per task. The same tasks are batched and",
                "executed in the same order; only queue allocation overhead is",
                "reduced. Existing worlds need a restart to replace their light",
                "task queue."
        })
        public boolean lightTaskQueueFastPath = true;
    }

    public static final class Performance {

        @ConfigComment({
                "Cache explosion line-of-sight exposure per (center, entity bounding box)",
                "within a single level tick. The vanilla algorithm re-traces ~27 rays per",
                "nearby entity per explosion through the same blocks, which is quadratic",
                "when many explosions share a tick (TNT chains, cannons).",
                "Behaviour note: explosions in the same tick share cached results, so an",
                "entity's cached exposure may predate block changes made by an earlier",
                "explosion in that tick. This matches Paper's optimize-explosions trade-off."
        })
        public volatile boolean explosionDensityCache = true;

        @ConfigComment({
                "Cache each entity's scoreboard team lookup, invalidated when team",
                "membership changes. Entity collision checks resolve the pusher's and",
                "every candidate's team once per pair; with hundreds of entities stacked",
                "together that is hundreds of thousands of scoreboard hash lookups per",
                "tick. Results are identical to vanilla."
        })
        public volatile boolean collisionTeamCache = true;

        @ConfigComment({
                "Cache block state, fluid resistance, and world-bounds checks per block",
                "position within a single explosion's ray pass. The 1352 explosion rays",
                "revisit each block in the blast sphere ~5 times on average, and no block",
                "mutates until after all rays finish, so results are identical to vanilla."
        })
        public volatile boolean explosionBlockCache = true;

        @ConfigComment({
                "Maximum TNT detonations processed per level tick. Set to 0 for vanilla",
                "behaviour (all fused TNT detonates in the same tick). When positive,",
                "TNT past the budget keeps its primed state and detonates on a following",
                "tick instead - nothing is cancelled, large chains are spread out so a",
                "15k-TNT detonation becomes a wave over a few seconds instead of a",
                "single multi-minute tick that trips the watchdog.",
                "Recommended starting point for cannon/stress servers: 200-500."
        })
        @ConfigRange(min = 0, max = 1_000_000)
        public volatile int tntDetonationBudget = 0;

        @ConfigComment({
                "Use a hash map for explosion drop merging instead of linear search.",
                "Vanilla loops over all collected drops to find merge targets (O(n²)).",
                "This switches to a hash-keyed lookup (O(n)), strictly vanilla-equivalent."
        })
        public volatile boolean explosionDropHashMerge = true;

        @ConfigComment({
                "Route explosion entity-exposure sight rays through the explosion block",
                "cache instead of chunk lookups. Each nearby entity costs ~27 line-of-",
                "sight rays of ~10 block steps; vanilla pays two chunk lookups per step.",
                "The blast sphere is already cached by the ray pass, blocks cannot",
                "change mid-explosion, and collision shapes are still resolved per",
                "entity - results are identical to vanilla."
        })
        public volatile boolean explosionExposureClipCache = true;

        @ConfigComment("Cache entity queries during explosions (avoids repeated AABB scans).")
        public volatile boolean explosionEntityCache = true;

        @ConfigComment({
                "Keep a separate hard-collision entity candidate index for",
                "movement checks. Normal entities only hard-collide with targets",
                "whose canBeCollidedWith method can return true; pushable-only",
                "living entities are still considered by vehicle and push logic.",
                "Results are identical to vanilla."
        })
        public volatile boolean entityHardCollisionCandidateIndex = true;

        @ConfigComment({
                "Scan entity sections by exact X/Z chunk columns before filtering",
                "section Y. Vanilla-compatible searches keep the same bounding",
                "grace but avoid walking unrelated Z columns that share an X key."
        })
        public volatile boolean entitySectionChunkScan = true;

        @ConfigComment({
                "Propagate abort results from entity collision candidate scans",
                "instead of allocating mutable holder objects on short-circuit",
                "queries. Results are identical to vanilla."
        })
        public volatile boolean entityCollisionAbortPropagation = true;

        @ConfigComment({
                "Use a direct consumer path for entity push scans when the caller",
                "does not need a stable result list. Callers that depend on list",
                "snapshot semantics, such as cramming checks, keep the vanilla",
                "collection path. Results are identical to vanilla."
        })
        public volatile boolean pushableEntityConsumer = true;

        @ConfigComment({
                "Use lazy collider iteration for single-axis entity movement so",
                "block/entity/world-border colliders do not need to be copied into",
                "one immutable list. Multi-axis and step-up movement keep the",
                "materialized vanilla path. Results are identical to vanilla."
        })
        public volatile boolean entityMovementLazyColliders = true;

        @ConfigComment({
                "Use a fast identity set and a no-passenger fast path in the",
                "entity tracker. Tracking range, visibility checks, and packet",
                "delivery are unchanged; only collection and stream overhead is",
                "reduced."
        })
        public volatile boolean entityTrackerFastPath = true;

        @ConfigComment({
                "Iterate deep passenger trees with one ordered iterator instead",
                "of recursive Stream pipelines. Self/passenger traversal order",
                "matches vanilla; only stream allocation overhead is reduced."
        })
        public volatile boolean deepPassengerIteration = true;

        @ConfigComment({
                "Use a direct loop for ClassInstanceMultiMap type-index",
                "creation instead of computeIfAbsent plus Stream. The cached",
                "type lists and returned read-only views are unchanged."
        })
        public volatile boolean entityTypeLookupFastPath = true;

        @ConfigComment({
                "Keep a per-section bit mask of positions that can receive a",
                "random block or fluid tick. Random positions are still selected",
                "with vanilla's randValue sequence; the mask only skips block",
                "state reads when the selected position cannot random tick."
        })
        public volatile boolean randomTickPositionMask = true;

        @ConfigComment({
                "Maintain a trigger-time priority queue for fluid scheduled-tick",
                "containers instead of scanning every loaded tick container each",
                "server tick. All due fluid ticks are still collected, ordered,",
                "and executed by vanilla's LevelTicks drain logic; this only",
                "removes scheduler lookup overhead when many chunks have pending",
                "fluid updates."
        })
        public volatile boolean fluidTickContainerQueue = true;

        @ConfigComment({
                "Collect horizontal fluid spread targets in fixed arrays instead",
                "of allocating an EnumMap for every flowing-fluid tick. The same",
                "directions are evaluated in vanilla order and the same targets",
                "are applied; this only reduces allocation and map overhead."
        })
        public volatile boolean flowingFluidSpreadArray = true;

        @ConfigComment({
                "Cache immutable chunk-generation status lists, pyramid steps,",
                "dependency radii, and loading dependency tables used by each",
                "chunk generation task. This avoids rebuilding the same control",
                "data while preserving the exact vanilla generation dependencies",
                "and execution order."
        })
        public volatile boolean chunkGenerationTaskPlanCache = true;

        @ConfigComment({
                "Schedule chunk task batches with a direct loop instead of stream",
                "pipelines and use a single-future fast path for one-task batches.",
                "The same tasks are submitted in the same order; only dispatcher",
                "allocation overhead is reduced."
        })
        public volatile boolean chunkTaskDispatcherBatchLoop = true;

        @ConfigComment({
                "Avoid opening or creating region files when chunk loads and bulk",
                "scans target a region file that does not exist, and scan blending",
                "old-chunk metadata for a whole region in one IOWorker task instead",
                "of scheduling 1024 per-chunk scan tasks. Existing region files,",
                "pending writes, datafixing, and old-chunk detection results are",
                "unchanged."
        })
        public volatile boolean chunkStorageRegionScanFastPath = true;

        @ConfigComment({
                "Lithium: cache NoiseBasedChunkGenerator sea level after the first",
                "settings lookup. Worldgen, structures, biome checks, and vanilla",
                "random sequences are unchanged; only repeated Holder.value()",
                "lookups are avoided."
        })
        public volatile boolean worldgenSeaLevelCache = true;

        @ConfigComment({
                "Use Fand's redstone wire engine for dust power propagation.",
                "The NMS wire hook enters Fand's implementation directly; the",
                "engine preserves vanilla power, event, block update, and",
                "neighbor notification order instead of delegating to Mojang's",
                "RedstoneWireEvaluator classes."
        })
        public volatile boolean fandRedstoneEngine = true;

        @ConfigComment({
                "Use a direct entity-section scan for item entity merging instead",
                "of allocating a nearby-item list before attempting merges.",
                "Merge rules and events are unchanged."
        })
        public volatile boolean itemEntityMergeFastPath = true;

        @ConfigComment({
                "Use direct loops and an entity-section scan for area effect cloud",
                "application instead of stream checks and a temporary nearby-entity",
                "list. AreaEffectCloudApplyEvent still receives the affected list."
        })
        public volatile boolean areaEffectCloudFastPath = true;

        @ConfigComment({
                "Find nearest AI targets while scanning entity sections instead of",
                "materializing a candidate list first. TargetingConditions and",
                "selection rules are unchanged."
        })
        public volatile boolean aiNearestTargetFastPath = true;

        @ConfigComment({
                "Replace hot AI goal stream/list post-processing with direct loops.",
                "This keeps the same selected entities while reducing per-goal",
                "allocation in dense mob scenes."
        })
        public volatile boolean aiGoalStreamFastPath = true;
    }

    public static final class Technical {

        @ConfigComment("Restore zero-tick growth behaviour for bamboo, cactus, chorus, sugar cane, kelp, and vines.")
        public volatile boolean zeroTickPlants = false;

        @ConfigComment("Restore old hopper item pickup behaviour where a full collision block above does not block loose item sucking.")
        public volatile boolean oldHopperSuckInBehavior = false;

        @ConfigComment("Allow dispenser shears to keep working after their durability reaches zero.")
        public volatile boolean shearsInDispenserCanZeroAmount = false;

        @ConfigComment("Allow entities with passengers, or riding entities, to use cross-dimension portals.")
        public volatile boolean allowEntityPortalWithPassenger = true;

        @ConfigComment("Do not place an End gateway portal ticket after teleporting entities.")
        public volatile boolean disableGatewayPortalEntityTicking = false;

        @ConfigComment("Allow removed living entities to continue through aiStep during the current tick.")
        public volatile boolean disableLivingEntityAiStepAliveCheck = false;

        @ConfigComment("Give freshly spawned players 60 ticks of damage immunity unless damage bypasses invulnerability.")
        public volatile boolean spawnInvulnerableTime = false;

        @ConfigComment("Restore old zombie piglin angry-drop credit behaviour.")
        public volatile boolean oldZombiePiglinDrop = false;

        @ConfigComment("Restore old zombie reinforcement spawning as normal zombies instead of the caller's exact zombie type.")
        public volatile boolean oldZombieReinforcement = false;

        @ConfigComment("Allow falling anvils to damage item entities.")
        public volatile boolean allowAnvilDestroyItemEntities = false;

        @ConfigComment("Disable clamping item damage values to the item's max damage.")
        public volatile boolean disableItemDamageCheck = false;

        @ConfigComment("Keep leash connections when a player boosts elytra flight with a firework rocket.")
        public volatile boolean keepLeashConnectWhenUseFirework = false;

        @ConfigComment("Restore old TNT-in-water behaviour where wet TNT explosions do not damage item/blocklike entities.")
        public volatile boolean tntWetExplosionNoItemDamage = false;

        @ConfigComment("Restore old projectile explosion knockback application by adding to delta movement instead of push().")
        public volatile boolean oldProjectileExplosionBehavior = false;

        @ConfigComment("Restore old throwable projectile tick order for snowballs, eggs, pearls, potions, and similar entities.")
        public volatile boolean oldThrowableProjectileTickOrder = false;

        @ConfigComment("Restore old minecart portal/passenger motion preservation behaviour.")
        public volatile boolean oldMinecartMotionBehavior = false;

        @ConfigComment("Restore copper bulb 1-game-tick redstone delay.")
        public volatile boolean copperBulbOneGameTickDelay = false;

        @ConfigComment("Restore crafter 1-game-tick trigger delay.")
        public volatile boolean crafterOneGameTickDelay = false;

        @ConfigComment("Prevent TNT from priming immediately when placed next to a powered source.")
        public volatile boolean noTntPlaceUpdate = false;

        @ConfigComment("Allow the vanilla piston desync duplication behaviour used by TNT, rail, and carpet dupers.")
        public volatile boolean allowPistonDuplication = true;

        @ConfigComment("Allow TNT dupers that rely on vanilla piston desync duplication.")
        public volatile boolean allowTntDuplication = true;

        @ConfigComment("Allow rail dupers that rely on vanilla piston desync duplication.")
        public volatile boolean allowRailDuplication = true;

        @ConfigComment("Allow carpet dupers that rely on vanilla piston desync duplication.")
        public volatile boolean allowCarpetDuplication = true;

        @ConfigComment("Allow falling blocks to duplicate through cross-dimension End portal teleportation.")
        public volatile boolean allowGravityBlockEndPortalDuplication = true;

        @ConfigComment("Restore old redstone wire/repeater/comparator behaviour that ignores upward support updates.")
        public volatile boolean redstoneIgnoreUpwardsUpdate = false;

        @ConfigComment("Allow pistons to move budding amethyst by treating its push reaction as normal.")
        public volatile boolean movableBuddingAmethyst = false;

        @ConfigComment("Allow tripwire hook/string duplication behaviour, including End platform drop compatibility.")
        public volatile boolean stringTripwireHookDuplicate = true;

        @ConfigComment({
                "End platform tripwire handling mode when stringTripwireHookDuplicate is enabled.",
                "Supported values: vanilla_21, vanilla_20, mixed."
        })
        public volatile String tripwireBehavior = "vanilla_21";
    }

    public static final class Console {

        @ConfigComment("Graphical server console window settings.")
        public final Gui gui = new Gui();
    }

    public static final class Gui {

        @ConfigComment("Show the graphical console window when a display is available and --nogui is not set.")
        public boolean enabled = true;

        @ConfigComment({
                "Initial colour theme for the GUI.",
                "Supported values: dark, light, system. Unknown values are rejected.",
                "The theme can also be switched at runtime from the GUI itself."
        })
        public String theme = "system";
    }

    public static final class Network {

        @ConfigComment("Proxy player information forwarding settings.")
        public final Forwarding forwarding = new Forwarding();
    }

    public static final class Forwarding {

        @ConfigComment({
                "Proxy forwarding mode.",
                "Supported values: none, bungee-legacy, velocity-modern."
        })
        public String mode = "none";

        @ConfigComment("Shared secret used by velocity-modern forwarding.")
        public String secret = "";
    }
}
