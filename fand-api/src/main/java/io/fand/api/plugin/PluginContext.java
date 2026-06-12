package io.fand.api.plugin;

import io.fand.api.command.CommandRegistry;
import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationService;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customitem.CustomItemRegistry;
import io.fand.api.event.EventBus;
import io.fand.api.gui.GuiService;
import io.fand.api.messaging.PluginMessaging;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.permission.PermissionService;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.storage.PluginStorage;
import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * Per-plugin runtime services. Provided by the server to {@link Plugin} callbacks.
 */
public interface PluginContext {

    PluginDescriptor descriptor();

    /** Logger pre-configured with the plugin's id as its name. */
    Logger logger();

    /** Event dispatcher scoped to this plugin's lifecycle. */
    EventBus events();

    /** Permission service visible to this plugin. */
    PermissionService permissions();

    /** Command registry scoped to this plugin's lifecycle. */
    CommandRegistry commands();

    /** Recipe registry scoped to this plugin's lifecycle. */
    RecipeRegistry recipes();

    /** Persistent scoreboard service scoped to this plugin's lifecycle. */
    ScoreboardService scoreboard();

    /** Packet registry scoped to this plugin's lifecycle. */
    PacketRegistry packets();

    /** Standard plugin messaging channels scoped to this plugin's lifecycle. */
    default PluginMessaging pluginMessaging() {
        return PluginMessaging.empty();
    }

    /** Custom item registry scoped to this plugin's lifecycle and namespace. */
    CustomItemRegistry customItems();

    /** Custom block registry scoped to this plugin's lifecycle and namespace. */
    CustomBlockRegistry customBlocks();

    /** Lightweight GUI service scoped to this plugin's lifecycle. */
    GuiService guis();

    /** Scheduler scoped to this plugin's lifecycle. */
    Scheduler scheduler();

    /** Persistent JSON/KV gameplay storage scoped to this plugin. */
    PluginStorage storage();

    /** Generic configuration loader for YAML, JSON, TOML, and properties files. */
    ConfigurationService configurations();

    /** Writable data directory unique to this plugin. Created on first access. */
    Path dataDirectory();

    /**
     * Plugin configuration backed by {@code dataDirectory()/config.yml}.
     *
     * <p>On first access, if no file exists yet, the bundled
     * {@code config.yml} from the plugin jar root is copied into place. If the
     * jar has no bundled config either, an empty document is created.
     */
    Configuration config();

    /**
     * Reloads {@link #config()} from disk, discarding any unsaved in-memory
     * changes. The returned instance is the same handle as {@link #config()}.
     */
    Configuration reloadConfig();
}
