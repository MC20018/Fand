package io.fand.server;

import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.advancement.AdvancementRegistry;
import io.fand.api.bossbar.BossBarService;
import io.fand.api.command.CommandRegistry;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customitem.CustomItemRegistry;
import io.fand.api.datapack.DataPackService;
import io.fand.api.enchantment.EnchantmentRegistry;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.gamerule.GameRuleService;
import io.fand.api.gui.GuiService;
import io.fand.api.integration.ExternalIntegrationStrategy;
import io.fand.api.loot.LootTableService;
import io.fand.api.map.MapService;
import io.fand.api.messaging.PluginMessaging;
import io.fand.api.lifecycle.LifecyclePhase;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.lifecycle.ServerStartingEvent;
import io.fand.api.lifecycle.ServerStoppingEvent;
import io.fand.api.nms.NmsService;
import io.fand.api.permission.PermissionService;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.placeholder.PlaceholderService;
import io.fand.api.player.PlayerAccessService;
import io.fand.api.player.SimulatedPlayerService;
import io.fand.api.region.RegionService;
import io.fand.api.plugin.PluginManager;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.service.ServiceRegistry;
import io.fand.api.structure.StructureService;
import io.fand.api.tag.TagRegistry;
import io.fand.api.tablist.TabListService;
import io.fand.api.text.MiniMessageService;
import io.fand.api.world.World;
import io.fand.api.world.WorldCreateOptions;
import io.fand.api.world.WorldTemplate;
import io.fand.api.world.generation.GenerationMode;
import io.fand.api.world.generation.VanillaBiomeSource;
import io.fand.server.block.FandCustomBlockRegistry;
import io.fand.server.advancement.FandAdvancementRegistry;
import io.fand.server.auth.FandLoginAuthenticationService;
import io.fand.server.auth.FandAuthEnvironment;
import io.fand.server.bossbar.FandBossBarService;
import io.fand.server.command.BuiltinCommands;
import io.fand.server.chunk.AsyncChunkPacketSender;
import io.fand.server.chunk.ChunkSendScheduler;
import io.fand.server.chunk.ChunkTrackingMetrics;
import io.fand.server.chunk.ChunkTaskExecutors;
import io.fand.server.command.CommandManager;
import io.fand.server.compat.modprotocol.ModProtocolCompatibility;
import io.fand.server.config.ConfigReloadResult;
import io.fand.server.config.ConfigReloader;
import io.fand.server.config.FandConfig;
import io.fand.server.datapack.FandDataPackService;
import io.fand.server.entity.EntityRegistry;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.enchantment.FandEnchantmentRegistry;
import io.fand.server.event.EventDispatcher;
import io.fand.server.gamerule.FandGameRuleService;
import io.fand.server.gui.FandGuiService;
import io.fand.server.item.FandCustomItemRegistry;
import io.fand.server.loot.FandLootTableService;
import io.fand.server.map.FandMapService;
import io.fand.server.messaging.FandPluginMessaging;
import io.fand.server.network.ProxyForwardingSettings;
import io.fand.server.nms.FandNmsService;
import io.fand.server.network.packet.PacketRegistryImpl;
import io.fand.server.placeholder.FandPlaceholderService;
import io.fand.server.permission.PermissionManager;
import io.fand.server.performance.ServerPerformanceTracker;
import io.fand.server.player.FandPlayerAccessService;
import io.fand.server.player.FandSimulatedPlayerService;
import io.fand.server.region.FandRegionService;
import io.fand.server.plugin.PluginRuntime;
import io.fand.server.recipe.FandRecipeRegistry;
import io.fand.server.scheduler.TaskScheduler;
import io.fand.server.scoreboard.FandScoreboardService;
import io.fand.server.service.FandServiceRegistry;
import io.fand.server.structure.FandStructureService;
import io.fand.server.tag.FandTagRegistry;
import io.fand.server.tag.FandTags;
import io.fand.server.tablist.FandTabListService;
import io.fand.server.text.FandMiniMessageService;
import io.fand.server.world.WorldRegistry;
import java.nio.file.Path;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.network.chat.Component;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandServer implements Server, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandServer.class);
    private static final long[] PLUGIN_CHANNEL_ADVERTISEMENT_RETRY_TICKS = {1L, 5L, 20L};

    private final Path configPath;
    private final EventDispatcher events;
    private final PermissionManager permissions;
    private final CommandManager commands;
    private final TaskScheduler scheduler;
    private final ChunkSendScheduler chunks;
    private final ChunkTaskExecutors chunkTasks;
    private final AsyncChunkPacketSender asyncChunkPackets;
    private final FandRecipeRegistry recipes;
    private final FandScoreboardService scoreboard;
    private final PacketRegistryImpl packets;
    private final FandPluginMessaging pluginMessaging;
    private final FandGameRuleService gameRules;
    private final FandRegionService regions;
    private final FandDataPackService dataPacks;
    private final ExternalIntegrationStrategy integrations;
    private final FandServiceRegistry services;
    private final FandNmsService nms;
    private final ModProtocolCompatibility modProtocols;
    private final EventSubscription pluginChannelAdvertisement;
    private final EventSubscription simulatedPlayerCleanup;
    private final FandCustomItemRegistry customItems;
    private final FandCustomBlockRegistry customBlocks;
    private final FandGuiService guis;
    private final FandLootTableService lootTables;
    private final FandAdvancementRegistry advancements;
    private final FandEnchantmentRegistry enchantments;
    private final FandStructureService structures;
    private final FandMapService maps;
    private final FandBossBarService bossBars;
    private final FandTabListService tabLists;
    private final FandPlaceholderService placeholders;
    private final FandMiniMessageService miniMessages;
    private final FandTagRegistry tags;
    private final PluginRuntime plugins;
    private final PlayerRegistry players;
    private final FandPlayerAccessService playerAccess;
    private final FandSimulatedPlayerService simulatedPlayers;
    private final FandLoginAuthenticationService loginAuthenticators;
    private final @Nullable Environment authEnvironment;
    private final ServerPerformanceTracker performance;
    private final ConfigReloader configReloader;
    private final ProxyForwardingSettings proxyForwarding;
    private final io.fand.server.console.gui.GuiThemeService guiThemes;
    private final Object configSaveLock = new Object();
    private final AtomicReference<WorldRegistry> worlds = new AtomicReference<>();
    private final AtomicReference<EntityRegistry> entities = new AtomicReference<>();
    private final AtomicReference<FandConfig> config;
    private final AtomicReference<LifecyclePhase> phase = new AtomicReference<>(LifecyclePhase.BOOTSTRAP);
    private final AtomicReference<MinecraftServer> minecraftServer = new AtomicReference<>();

    public FandServer() {
        this(Path.of("fand.yml"), FandConfig.load(Path.of("fand.yml")), Main.class.getClassLoader());
    }

    FandServer(FandConfig config, ClassLoader parentClassLoader) {
        this(Path.of("fand.yml"), config, parentClassLoader);
    }

    FandServer(Path configPath, FandConfig initialConfig, ClassLoader parentClassLoader) {
        this.configPath = configPath;
        this.config = new AtomicReference<>(initialConfig);
        this.proxyForwarding = ProxyForwardingSettings.fromConfig(initialConfig);
        this.guiThemes = new io.fand.server.console.gui.GuiThemeService(
                io.fand.server.console.gui.GuiTheme.fromConfig(initialConfig.console.gui.theme),
                this::persistGuiTheme);
        this.events = new EventDispatcher();
        this.permissions = new PermissionManager(events);
        this.commands = new CommandManager(permissions);
        registerBuiltinCommands();
        this.scheduler = new TaskScheduler(initialConfig.scheduler.asyncThreads, initialConfig.scheduler.regionThreads);
        this.chunks = new ChunkSendScheduler(initialConfig.chunks);
        this.chunkTasks = new ChunkTaskExecutors(initialConfig.chunks);
        this.asyncChunkPackets = new AsyncChunkPacketSender(initialConfig.chunks.asyncChunkPacketPreparation);
        this.recipes = new FandRecipeRegistry();
        this.scoreboard = new FandScoreboardService(minecraftServer::get);
        this.tabLists = new FandTabListService(minecraftServer::get);
        this.packets = new PacketRegistryImpl(minecraftServer::get);
        this.players = new PlayerRegistry(permissions, scoreboard, tabLists);
        this.pluginMessaging = new FandPluginMessaging(packets, players::snapshot);
        this.gameRules = new FandGameRuleService();
        this.regions = new FandRegionService(Path.of("regions"));
        this.dataPacks = new FandDataPackService(Path.of("datapacks"), minecraftServer::get);
        this.integrations = ExternalIntegrationStrategy.empty();
        this.services = new FandServiceRegistry();
        this.nms = new FandNmsService(minecraftServer::get);
        this.pluginChannelAdvertisement = events.subscribe(
                PlayerJoinEvent.class,
                EventPriority.OBSERVER,
                this::advertisePluginChannels);
        this.customItems = new FandCustomItemRegistry();
        this.customBlocks = new FandCustomBlockRegistry(events, customItems);
        this.guis = new FandGuiService(events);
        this.lootTables = new FandLootTableService(minecraftServer::get);
        this.advancements = new FandAdvancementRegistry(minecraftServer::get);
        this.enchantments = new FandEnchantmentRegistry(minecraftServer::get);
        this.structures = new FandStructureService(minecraftServer::get);
        this.modProtocols = new ModProtocolCompatibility(pluginMessaging, events, structures, initialConfig.compat.modProtocols);
        this.maps = new FandMapService(minecraftServer::get);
        this.bossBars = new FandBossBarService(minecraftServer::get, scheduler);
        this.placeholders = new FandPlaceholderService();
        this.miniMessages = new FandMiniMessageService(placeholders);
        this.tags = new FandTagRegistry();
        this.playerAccess = new FandPlayerAccessService(minecraftServer::get);
        this.simulatedPlayers = new FandSimulatedPlayerService(minecraftServer::get, players, events);
        this.loginAuthenticators = new FandLoginAuthenticationService();
        this.loginAuthenticators.builtin(this::verifyLoginSession);
        this.authEnvironment = "third-party".equals(initialConfig.authentication.mode)
                ? FandAuthEnvironment.fromConfig(initialConfig.authentication)
                : null;
        this.simulatedPlayerCleanup = events.subscribe(
                PlayerQuitEvent.class,
                EventPriority.OBSERVER,
                event -> this.simulatedPlayers.forgetIfSimulated(event.player().uniqueId()));
        this.performance = new ServerPerformanceTracker(() -> chunks.metrics().pendingJobs());
        var pluginDirectory = Path.of(initialConfig.plugins.directory);
        this.plugins = new PluginRuntime(
                pluginDirectory,
                pluginDirectory,
                parentClassLoader,
                commands,
                events,
                permissions,
                scheduler,
                recipes,
                lootTables,
                scoreboard,
                packets,
                pluginMessaging,
                regions,
                dataPacks,
                advancements,
                enchantments,
                structures,
                maps,
                bossBars,
                tabLists,
                placeholders,
                simulatedPlayers,
                customItems,
                customBlocks,
                guis,
                ConfigReloader.toPluginOptions(initialConfig)
        );
        this.plugins.gameRuleService(gameRules);
        this.plugins.integrations(integrations);
        this.plugins.serviceRegistry(services);
        this.plugins.nmsService(nms);
        this.plugins.loginAuthenticationService(loginAuthenticators);
        this.configReloader = new ConfigReloader(configPath, config, plugins, scheduler, chunks, chunkTasks, guiThemes);
        io.fand.server.hooks.FandHooks.applyPlayerConfig(initialConfig.players);
        io.fand.server.hooks.FandHooks.applyAuthenticationConfig(initialConfig.authentication);
        io.fand.server.hooks.FandHooks.applyChunkConfig(initialConfig.chunks);
        io.fand.server.hooks.FandHooks.applyPerformanceConfig(initialConfig.performance);
        io.fand.server.hooks.FandHooks.applyTechnicalConfig(initialConfig.technical);
    }

    /**
     * Binds the API singleton and runs plugin discovery + onLoad. Called before
     * the vanilla server is constructed so plugins observe a coherent runtime
     * before any world data exists.
     */
    public void start() {
        if (!phase.compareAndSet(LifecyclePhase.BOOTSTRAP, LifecyclePhase.LOADED)) {
            throw new IllegalStateException("Fand runtime already started, current phase: " + phase.get());
        }
        Fand.bind(this);
        plugins.loadPlugins();
        LOGGER.info("Loaded Fand {} for Minecraft {}", version(), minecraftVersion());
    }

    /**
     * Runs after the vanilla server is initialized but before it begins ticking.
     * Fires {@link ServerStartingEvent}, enables plugins, then fires
     * {@link ServerStartedEvent}.
     */
    public void enable() {
        if (!phase.compareAndSet(LifecyclePhase.LOADED, LifecyclePhase.STARTING)) {
            throw new IllegalStateException("enable() requires LOADED phase, was: " + phase.get());
        }
        try {
            events.fire(new ServerStartingEvent(this));
            plugins.enablePlugins();
            fireWorldLoadEvents();
            recipes.applyLoadedRecipes();
            phase.set(LifecyclePhase.RUNNING);
            events.fire(new ServerStartedEvent(this));
        } catch (Throwable failure) {
            LOGGER.error("Fand enable() failed; shutting down the vanilla server", failure);
            shutdown("Fand enable() failed: " + failure.getMessage());
            throw failure;
        }
    }

    public ConfigReloadResult reloadConfig() {
        synchronized (configSaveLock) {
            return configReloader.reload();
        }
    }

    public ProxyForwardingSettings proxyForwarding() {
        return proxyForwarding;
    }

    public FandConfig config() {
        return config.get();
    }

    public FandLoginAuthenticationService loginAuthenticators() {
        return loginAuthenticators;
    }

    public @Nullable Environment authEnvironment() {
        return authEnvironment;
    }

    private FandLoginAuthenticationService.LoginAttempt verifyLoginSession(io.fand.api.auth.LoginAuthenticationRequest request) {
        var server = minecraftServer.get();
        if (server == null) {
            return FandLoginAuthenticationService.LoginAttempt.pass();
        }

        var name = request.name();
        try {
            ProfileResult result = server.services()
                    .sessionService()
                    .hasJoinedServer(name, request.serverId(), request.authenticationAddressOrNull());
            if (result != null) {
                return acceptAuthenticatedProfile(result.profile());
            }
            if (server.isSingleplayer()) {
                LOGGER.warn("Failed to verify username but will let them in anyway!");
                return acceptAuthenticatedProfile(UUIDUtil.createOfflineProfile(name));
            }
            LOGGER.error("Username '{}' tried to join with an invalid session", name);
            return FandLoginAuthenticationService.LoginAttempt.deny(Component.translatable("multiplayer.disconnect.unverified_username"));
        } catch (AuthenticationUnavailableException ignored) {
            if (server.isSingleplayer()) {
                LOGGER.warn("Authentication servers are down but will let them in anyway!");
                return acceptAuthenticatedProfile(UUIDUtil.createOfflineProfile(name));
            }
            LOGGER.error("Couldn't verify username because servers are unavailable");
            return FandLoginAuthenticationService.LoginAttempt.deny(Component.translatable("multiplayer.disconnect.authservers_down"));
        }
    }

    private FandLoginAuthenticationService.LoginAttempt acceptAuthenticatedProfile(GameProfile profile) {
        LOGGER.info("UUID of player {} is {}", profile.name(), profile.id());
        return FandLoginAuthenticationService.LoginAttempt.allow(profile);
    }

    public boolean consoleGuiEnabled() {
        return config.get().console.gui.enabled;
    }

    public io.fand.server.console.gui.GuiThemeService guiThemes() {
        return guiThemes;
    }

    private void persistGuiTheme(io.fand.server.console.gui.GuiTheme theme) {
        Objects.requireNonNull(theme, "theme");
        synchronized (configSaveLock) {
            var persisted = FandConfig.load(configPath);
            persisted.console.gui.theme = theme.configValue();
            FandConfig.save(configPath, persisted);
            config.get().console.gui.theme = theme.configValue();
        }
    }

    public void attach(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!minecraftServer.compareAndSet(null, server)) {
            throw new IllegalStateException("Minecraft server is already attached");
        }
        var registry = new WorldRegistry(server, players, scheduler, gameRules);
        worlds.set(registry);
        entities.set(registry.entityRegistry());
        players.bindWorldRegistry(registry);
        players.bindWorldResolver(registry::wrap);
        io.fand.server.item.FandItemStacks.useRegistries(server.registryAccess());
        recipes.bind(server);
        advancements.applyLoadedAdvancements();
        enchantments.applyLoadedEnchantments();
        structures.applyLoadedStructures();
    }

    /**
     * Blocks until the attached vanilla server thread terminates. Called by
     * Main after net.minecraft.server.Main.main returns so the launcher
     * (Fandclip) does not close the plugin classloader while the server is
     * still running.
     */
    public void awaitMinecraftServerStop() {
        var server = minecraftServer.get();
        if (server == null) {
            return;
        }
        try {
            server.getRunningThread().join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public void tickScheduler() {
        if (phase.get() == LifecyclePhase.STOPPING || phase.get() == LifecyclePhase.STOPPED) {
            return;
        }
        scheduler.tick();
        recipes.tick();
        maps.tick();
        var server = minecraftServer.get();
        if (server != null) {
            modProtocols.tick(server);
        }
        for (var world : worlds()) {
            customBlocks.tick(world);
        }
    }

    public ChunkSendScheduler chunkSendScheduler() {
        return chunks;
    }

    public ChunkTrackingMetrics chunkTrackingMetrics() {
        return chunks.metrics();
    }

    public AsyncChunkPacketSender asyncChunkPackets() {
        return asyncChunkPackets;
    }

    public java.util.concurrent.Executor chunkBackgroundExecutor() {
        return this.chunkTasks.loadExecutor();
    }

    public java.util.concurrent.Executor chunkWorldgenExecutor() {
        return this.chunkTasks.worldgenExecutor();
    }

    public void recordTick(long tickStartNanos, long tickDurationNanos) {
        performance.recordTick(tickStartNanos, tickDurationNanos);
    }

    public void recordTick(long tickStartNanos, long tickDurationNanos, long taskExecutionNanos) {
        performance.recordTick(tickStartNanos, tickDurationNanos, taskExecutionNanos);
    }

    @Override
    public String brand() {
        return config.get().identity.brand;
    }

    @Override
    public String version() {
        return BuildInfo.VERSION;
    }

    @Override
    public String minecraftVersion() {
        return BuildInfo.MINECRAFT_VERSION;
    }

    @Override
    public PluginManager plugins() {
        return plugins;
    }

    public PluginRuntime pluginRuntime() {
        return plugins;
    }

    @Override
    public EventBus events() {
        return events;
    }

    @Override
    public PermissionService permissions() {
        return permissions;
    }

    public CommandManager commandManager() {
        return commands;
    }

    @Override
    public CommandRegistry commands() {
        return commands;
    }

    @Override
    public RecipeRegistry recipes() {
        return recipes;
    }

    @Override
    public ScoreboardService scoreboard() {
        return scoreboard;
    }

    @Override
    public PacketRegistry packets() {
        return packets;
    }

    @Override
    public PluginMessaging pluginMessaging() {
        return pluginMessaging;
    }

    @Override
    public GameRuleService gameRules() {
        return gameRules;
    }

    @Override
    public RegionService regions() {
        return regions;
    }

    @Override
    public DataPackService dataPacks() {
        return dataPacks;
    }

    @Override
    public ExternalIntegrationStrategy integrations() {
        return integrations;
    }

    @Override
    public ServiceRegistry services() {
        return services;
    }

    @Override
    public NmsService nms() {
        return nms;
    }

    @Override
    public CustomItemRegistry customItems() {
        return customItems;
    }

    @Override
    public CustomBlockRegistry customBlocks() {
        return customBlocks;
    }

    public FandCustomBlockRegistry customBlockRegistry() {
        return customBlocks;
    }

    @Override
    public LootTableService lootTables() {
        return lootTables;
    }

    @Override
    public AdvancementRegistry advancements() {
        return advancements;
    }

    @Override
    public EnchantmentRegistry enchantments() {
        return enchantments;
    }

    @Override
    public StructureService structures() {
        return structures;
    }

    @Override
    public MapService maps() {
        return maps;
    }

    @Override
    public BossBarService bossBars() {
        return bossBars;
    }

    @Override
    public TabListService tabLists() {
        return tabLists;
    }

    @Override
    public PlaceholderService placeholders() {
        return placeholders;
    }

    @Override
    public MiniMessageService miniMessages() {
        return miniMessages;
    }

    @Override
    public TagRegistry tags() {
        return tags;
    }

    @Override
    public GuiService guis() {
        return guis;
    }

    public PacketRegistryImpl packetRegistry() {
        return packets;
    }

    public FandTabListService tabListService() {
        return tabLists;
    }

    public void addPluginChannelConfigurationTask(java.util.Queue<net.minecraft.server.network.ConfigurationTask> tasks) {
        tasks.add(pluginMessaging.pluginChannelConfigurationTask());
    }

    public boolean handlePluginChannelConfigurationPayload(
            net.minecraft.server.network.ServerConfigurationPacketListenerImpl listener,
            net.minecraft.resources.Identifier id,
            byte[] payload
    ) {
        return pluginMessaging.handleConfigurationPayload(listener, id, payload);
    }

    public void syncDataPackContents(net.minecraft.server.level.ServerPlayer player, boolean joined) {
        modProtocols.syncDataPackContents(player, joined);
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public int onlinePlayers() {
        var server = minecraftServer.get();
        return server == null ? 0 : server.getPlayerCount();
    }

    @Override
    public int maxPlayers() {
        var server = minecraftServer.get();
        return server == null ? -1 : server.getMaxPlayers();
    }

    @Override
    public io.fand.api.performance.ServerPerformance performance() {
        return performance.snapshot();
    }

    @Override
    public void broadcast(net.kyori.adventure.text.Component message) {
        Objects.requireNonNull(message, "message");
        players.snapshot().forEach(player -> player.sendMessage(message));
    }

    @Override
    public int currentTick() {
        var server = minecraftServer.get();
        return server == null ? Server.super.currentTick() : server.getTickCount();
    }

    @Override
    public String motd() {
        var server = minecraftServer.get();
        return server == null ? "" : server.getMotd();
    }

    @Override
    public void setMotd(String motd) {
        Objects.requireNonNull(motd, "motd");
        var server = minecraftServer.get();
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        server.setMotd(motd);
    }

    @Override
    public CompletableFuture<Boolean> reloadData() {
        var server = minecraftServer.get();
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is not attached"));
        }
        return dataPacks.reload();
    }

    @Override
    public Collection<? extends io.fand.api.entity.Player> players() {
        return players.snapshot();
    }

    @Override
    public PlayerAccessService playerAccess() {
        return playerAccess;
    }

    @Override
    public SimulatedPlayerService simulatedPlayers() {
        return simulatedPlayers;
    }

    @Override
    public Iterable<? extends net.kyori.adventure.audience.Audience> audiences() {
        return players.snapshot();
    }

    @Override
    public Optional<? extends io.fand.api.entity.Player> player(UUID uniqueId) {
        return players.find(uniqueId);
    }

    @Override
    public Optional<? extends io.fand.api.entity.Player> player(String name) {
        return players.findByName(name);
    }

    @Override
    public Optional<? extends io.fand.api.entity.Entity> entity(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        var player = players.find(uniqueId);
        if (player.isPresent()) {
            return player;
        }
        var registry = worlds.get();
        if (registry == null) {
            return Optional.empty();
        }
        for (var world : registry.snapshot()) {
            var found = world.entity(uniqueId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public PlayerRegistry playerRegistry() {
        return players;
    }

    @Override
    public Collection<? extends io.fand.api.world.World> worlds() {
        var registry = worlds.get();
        return registry == null ? List.of() : registry.snapshot();
    }

    @Override
    public Optional<? extends io.fand.api.world.World> world(Key key) {
        var registry = worlds.get();
        return registry == null ? Optional.empty() : registry.find(key);
    }

    @Override
    public Optional<? extends io.fand.api.world.World> defaultWorld() {
        var registry = worlds.get();
        return registry == null ? Optional.empty() : registry.defaultWorld();
    }

    @Override
    public CompletableFuture<World> createWorld(Key key, WorldTemplate template) {
        return createWorld(key, WorldCreateOptions.of(template));
    }

    @Override
    public CompletableFuture<World> createWorld(Key key, WorldCreateOptions options) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(options, "options");
        var server = minecraftServer.get();
        var registry = worlds.get();
        if (server == null || registry == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is not attached"));
        }
        if (server.isStopped()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is stopping"));
        }
        return server.submit(() -> {
            var level = server.fand$createDynamicLevel(dimensionKey(key), levelStem(server, key, options));
            World world = registry.wrap(level);
            events.fire(new WorldLoadEvent(world));
            return world;
        });
    }

    @Override
    public CompletableFuture<Boolean> unloadWorld(Key key) {
        Objects.requireNonNull(key, "key");
        var server = minecraftServer.get();
        var registry = worlds.get();
        if (server == null || registry == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is not attached"));
        }
        if (server.isStopped()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft server is stopping"));
        }
        return server.submit(() -> {
            var world = registry.find(key).map(FandServer::requireFandWorld).orElse(null);
            if (world == null) {
                return false;
            }
            events.fire(new WorldUnloadEvent(world));
            boolean unloaded = server.fand$unloadDynamicLevel(dimensionKey(key));
            if (unloaded) {
                registry.forget(world);
            }
            return unloaded;
        });
    }

    public Optional<WorldRegistry> worldRegistry() {
        return Optional.ofNullable(worldRegistryOrNull());
    }

    public @Nullable WorldRegistry worldRegistryOrNull() {
        return worlds.get();
    }

    public Optional<EntityRegistry> entityRegistry() {
        return Optional.ofNullable(entityRegistryOrNull());
    }

    public @Nullable EntityRegistry entityRegistryOrNull() {
        return entities.get();
    }

    @Override
    public Optional<? extends io.fand.api.block.BlockType> blockType(Key key) {
        Objects.requireNonNull(key, "key");
        return blockRegistry().getOptional(identifier(key))
                .map(io.fand.server.block.FandBlockType::of);
    }

    @Override
    public Optional<? extends io.fand.api.tag.Tag<io.fand.api.block.BlockType>> blockTag(Key key) {
        Objects.requireNonNull(key, "key");
        return blockRegistry().get(FandTags.blockTagKey(key)).map(FandTags::block);
    }

    @Override
    public Collection<? extends io.fand.api.tag.Tag<io.fand.api.block.BlockType>> blockTags() {
        return blockRegistry().getTags().map(FandTags::block).toList();
    }

    @Override
    public Optional<? extends io.fand.api.item.ItemType> itemType(Key key) {
        Objects.requireNonNull(key, "key");
        return itemRegistry().getOptional(identifier(key))
                .map(io.fand.server.item.FandItemType::of);
    }

    @Override
    public Optional<? extends io.fand.api.tag.Tag<io.fand.api.item.ItemType>> itemTag(Key key) {
        Objects.requireNonNull(key, "key");
        return itemRegistry().get(FandTags.itemTagKey(key)).map(FandTags::item);
    }

    @Override
    public Collection<? extends io.fand.api.tag.Tag<io.fand.api.item.ItemType>> itemTags() {
        return itemRegistry().getTags().map(FandTags::item).toList();
    }

    @Override
    public Optional<? extends io.fand.api.entity.EntityType> entityType(Key key) {
        Objects.requireNonNull(key, "key");
        return entityTypeRegistry().getOptional(identifier(key))
                .map(io.fand.server.entity.FandEntityType::of);
    }

    @Override
    public Optional<? extends io.fand.api.tag.Tag<io.fand.api.entity.EntityType>> entityTypeTag(Key key) {
        Objects.requireNonNull(key, "key");
        return entityTypeRegistry().get(FandTags.entityTypeTagKey(key)).map(FandTags::entityType);
    }

    @Override
    public Collection<? extends io.fand.api.tag.Tag<io.fand.api.entity.EntityType>> entityTypeTags() {
        return entityTypeRegistry().getTags().map(FandTags::entityType).toList();
    }

    @Override
    public LifecyclePhase phase() {
        return phase.get();
    }

    @Override
    public void shutdown(@Nullable String reason) {
        LOGGER.info("Shutdown requested: {}", reason == null ? "<no reason>" : reason);
        var server = minecraftServer.get();
        if (server == null) {
            close();
            return;
        }
        server.halt(false);
    }

    @Override
    public io.fand.api.inventory.Inventory createInventory(
            io.fand.api.inventory.InventoryType type,
            int size,
            net.kyori.adventure.text.Component title) {
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(title, "title");
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0, got " + size);
        }
        if (type == io.fand.api.inventory.InventoryType.PLAYER
                || type == io.fand.api.inventory.InventoryType.UNKNOWN) {
            throw new IllegalArgumentException("Cannot create a standalone inventory of type " + type);
        }
        var probe = io.fand.server.inventory.OpenableContainers.build(type, size);
        if (probe == null) {
            throw new IllegalArgumentException(
                    "InventoryType " + type + " cannot be opened standalone — needs a backing block");
        }
        int resolvedSize = probe.container().getContainerSize();
        return new io.fand.server.inventory.FandInventory(type, resolvedSize, title);
    }

    @Override
    public void close() {
        LifecyclePhase current;
        while (true) {
            current = phase.get();
            if (current == LifecyclePhase.STOPPING || current == LifecyclePhase.STOPPED) {
                return;
            }
            if (phase.compareAndSet(current, LifecyclePhase.STOPPING)) {
                break;
            }
        }

        if (current == LifecyclePhase.RUNNING || current == LifecyclePhase.STARTING) {
            try {
                events.fire(new ServerStoppingEvent(this, null));
            } catch (RuntimeException failure) {
                LOGGER.warn("ServerStoppingEvent listener failed", failure);
            }
            fireWorldUnloadEvents();
        }
        plugins.disablePlugins();
        plugins.close();
        services.close();
        pluginChannelAdvertisement.close();
        simulatedPlayerCleanup.close();
        modProtocols.close();
        pluginMessaging.close();
        bossBars.close();
        tabLists.close();
        simulatedPlayers.close();
        placeholders.close();
        packets.close();
        guis.close();
        asyncChunkPackets.close();
        chunks.close();
        chunkTasks.close();
        scheduler.close();
        guiThemes.close();
        performance.close();
        Fand.unbind(this);
        phase.set(LifecyclePhase.STOPPED);
        LOGGER.info("Fand runtime stopped");
    }

    private void registerBuiltinCommands() {
        BuiltinCommands.registerAll(commands, this);
    }

    private void advertisePluginChannels(PlayerJoinEvent event) {
        var player = event.player();
        var uniqueId = player.uniqueId();
        pluginMessaging.advertise(player);
        for (long delayTicks : PLUGIN_CHANNEL_ADVERTISEMENT_RETRY_TICKS) {
            scheduler.runMainAfterTicks(() -> player(uniqueId).ifPresent(pluginMessaging::advertise), delayTicks);
        }
    }

    private static ResourceKey<Level> dimensionKey(Key key) {
        return ResourceKey.create(Registries.DIMENSION, identifier(key));
    }

    private static ResourceKey<LevelStem> templateKey(WorldTemplate template) {
        return ResourceKey.create(Registries.LEVEL_STEM, identifier(template.key()));
    }

    private LevelStem levelStem(MinecraftServer server, Key worldKey, WorldCreateOptions options) {
        var template = templateKey(options.template());
        if (options.isVoidWorld()) {
            return server.fand$voidLevelStem(template);
        }
        var generator = options.generator().orElse(null);
        var dimensions = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
        var base = dimensions.getValue(template);
        if (base == null) {
            throw new IllegalArgumentException("World template is not available: " + template.identifier());
        }
        if (generator == null) {
            return base;
        }
        if (options.generatorSettings().mode() == GenerationMode.TEMPLATE) {
            return base;
        }
        var settings = options.generatorSettings();
        io.fand.server.world.FandBiomeRegistry.applyCustomBiomes(server, settings);
        var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<DimensionType> dimensionType = base.type();
        var dimensionTypeOverride = settings.dimensionType();
        if (dimensionTypeOverride.isPresent()) {
            var key = dimensionTypeOverride.get();
            dimensionType = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE)
                    .get(dimensionTypeKey(key))
                    .orElseThrow(() -> new IllegalArgumentException("Dimension type is not available: " + key.asString()));
        }
        long seed = server.getWorldGenSettings().options().seed();
        if (settings.usesVanillaNoisePipeline()) {
            BiomeSource biomeSource = settings.biomeSource() == VanillaBiomeSource.TEMPLATE
                    ? base.generator().getBiomeSource()
                    : new io.fand.server.world.FandBiomeSource(
                            biomes,
                            settings.biomeProvider(),
                            biomes.getOrThrow(Biomes.PLAINS));
            return new LevelStem(
                    dimensionType,
                    new io.fand.server.world.FandVanillaWorldGeneratorSource(
                            worldKey,
                            seed,
                            biomes,
                            biomeSource,
                            noiseSettings(server, base, settings.noiseSettings().orElse(null)),
                            generator,
                            settings,
                            structures));
        }
        return new LevelStem(
                dimensionType,
                new io.fand.server.world.FandWorldGeneratorSource(
                        worldKey,
                        seed,
                        biomes,
                        generator,
                        settings,
                        structures));
    }

    private static Holder<NoiseGeneratorSettings> noiseSettings(
            MinecraftServer server,
            LevelStem base,
            @Nullable Key override
    ) {
        if (override != null) {
            return server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                    .get(noiseSettingsKey(override))
                    .orElseThrow(() -> new IllegalArgumentException("Noise settings are not available: " + override.asString()));
        }
        if (base.generator() instanceof NoiseBasedChunkGenerator noiseGenerator) {
            return noiseGenerator.generatorSettings();
        }
        return server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD);
    }

    private static ResourceKey<DimensionType> dimensionTypeKey(Key key) {
        return ResourceKey.create(Registries.DIMENSION_TYPE, identifier(key));
    }

    private static ResourceKey<NoiseGeneratorSettings> noiseSettingsKey(Key key) {
        return ResourceKey.create(Registries.NOISE_SETTINGS, identifier(key));
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private Registry<Block> blockRegistry() {
        var server = minecraftServer.get();
        return server == null ? BuiltInRegistries.BLOCK : server.registryAccess().lookupOrThrow(Registries.BLOCK);
    }

    private Registry<Item> itemRegistry() {
        var server = minecraftServer.get();
        return server == null ? BuiltInRegistries.ITEM : server.registryAccess().lookupOrThrow(Registries.ITEM);
    }

    private Registry<net.minecraft.world.entity.EntityType<?>> entityTypeRegistry() {
        var server = minecraftServer.get();
        return server == null
                ? BuiltInRegistries.ENTITY_TYPE
                : server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
    }

    private static io.fand.server.world.FandWorld requireFandWorld(io.fand.api.world.World world) {
        if (world instanceof io.fand.server.world.FandWorld fandWorld) {
            return fandWorld;
        }
        throw new IllegalArgumentException("World is not owned by this server: " + world.key().asString());
    }

    private void fireWorldLoadEvents() {
        for (var world : worlds()) {
            events.fire(new WorldLoadEvent(world));
        }
    }

    private void fireWorldUnloadEvents() {
        for (var world : worlds()) {
            try {
                events.fire(new WorldUnloadEvent(world));
            } catch (RuntimeException failure) {
                LOGGER.warn("WorldUnloadEvent listener failed for {}", world.key().asString(), failure);
            }
        }
    }
}
