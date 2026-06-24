package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.command.CommandArgument;
import io.fand.api.command.CommandArgumentType;
import io.fand.api.command.CommandDescriptor;
import io.fand.server.command.CommandManager;
import io.fand.server.command.CommandTreeBridge;
import io.fand.server.event.EventDispatcher;
import io.fand.server.permission.PermissionManager;
import io.fand.server.permission.PermissionSet;
import io.fand.server.scheduler.TaskScheduler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PluginCommandIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void scopedPluginCommandsPreserveTypedArguments() {
        var commands = new CommandManager(new PermissionManager());
        var registry = new PluginCommandRegistry(commands, new PluginResourceTracker(), "demo");

        registry.register(
                new CommandDescriptor(
                        "ignored",
                        "give",
                        List.of(),
                        List.of("target"),
                        List.of(CommandArgument.players("target")),
                        List.of(),
                        null),
                (sender, label, args) -> {},
                (sender, label, args) -> List.of());

        var descriptor = commands.lookup("demo:give").orElseThrow().descriptor();
        assertThat(descriptor.namespace()).isEqualTo("demo");
        assertThat(descriptor.typedArguments()).hasSize(1);
        assertThat(descriptor.typedArguments().getFirst().type()).isEqualTo(CommandArgumentType.PLAYERS);
    }

    @Test
    void pluginCommandRegistryOnlyResolvesOwnCommands() {
        var commands = new CommandManager(new PermissionManager());
        var demo = new PluginCommandRegistry(commands, new PluginResourceTracker(), "demo");
        var other = new PluginCommandRegistry(commands, new PluginResourceTracker(), "other");
        var sender = new Sender(true);

        demo.register(new CommandDescriptor("ignored", "own", List.of(), List.of(), List.of(), List.of(), null), (current, label, args) -> {}, (current, label, args) -> List.of("owned"));
        other.register(new CommandDescriptor("ignored", "foreign", List.of(), List.of(), List.of(), List.of(), null), (current, label, args) -> {}, (current, label, args) -> List.of("hidden"));

        assertThat(demo.resolve(sender, List.of("own"))).isPresent();
        assertThat(demo.resolve(sender, List.of("demo:own"))).isPresent();
        assertThat(demo.resolve(sender, List.of("foreign"))).isEmpty();
        assertThat(demo.resolve(sender, List.of("other:foreign"))).isEmpty();
        assertThat(demo.suggestions(sender, List.of(""))).contains("own", "demo:own").doesNotContain("foreign", "other:foreign");
        assertThat(demo.suggestions(sender, List.of("foreign", ""))).isEmpty();
        assertThat(demo.claims(List.of("own"))).isTrue();
        assertThat(demo.claims(List.of("foreign"))).isFalse();
    }

    @Test
    void pluginAnnotatedCommandExecutesCompletesAndUnregistersOnDisable() throws Exception {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        var logFile = tempDir.resolve("commands.log");
        var previousLog = System.getProperty("fand.plugin.test.log");
        System.setProperty("fand.plugin.test.log", logFile.toString());
        try {
            PluginRuntimeTestSupport.createPluginJar(
                    tempDir,
                    pluginsDir.resolve("demo.jar"),
                    PluginRuntimeTestSupport.descriptorJson("demo", "testplugins.demo.DemoPlugin", List.of()),
                    Map.of("testplugins/demo/DemoPlugin.java", pluginSource()),
                    List.of()
            );

            var commands = new CommandManager(new PermissionManager());
            var runtime = new PluginRuntime(
                    pluginsDir,
                    pluginsDir,
                    getClass().getClassLoader(),
                    commands,
                    new EventDispatcher(),
                    new PermissionManager(),
                    new TaskScheduler()
            );
            var denied = new Sender(false);
            var allowed = new Sender(true);
            try {
                runtime.loadPlugins();
                runtime.enablePlugins();

                assertThat(commands.resolve(denied, List.of("hello", "world"))).isEmpty();
                assertThat(commands.resolve(allowed, List.of("hello", "world"))).isPresent();
                assertThat(commands.suggestions(allowed, List.of("hello", "w"))).containsExactly("world");
                assertThat(commands.suggestions(allowed, List.of("hello", "world", "a"))).containsExactly("alpha", "beta");

                var resolved = commands.resolve(allowed, List.of("hello", "world", "alpha")).orElseThrow();
                resolved.command().executor().execute(allowed, resolved.usedLabel(), List.of("alpha"));

                var root = new com.mojang.brigadier.tree.RootCommandNode<net.minecraft.commands.CommandSourceStack>();
                CommandTreeBridge.appendToRoot(commands, allowed, root);
                assertThat(root.getChild("hello")).isNotNull();
                assertThat(root.getChild("demo:hello")).isNotNull();
            } finally {
                runtime.close();
            }

            assertThat(commands.resolve(allowed, List.of("demo", "hello", "world"))).isEmpty();
            assertThat(Files.readAllLines(logFile)).containsExactly("command:alpha");
        } finally {
            PluginRuntimeTestSupport.restoreProperty("fand.plugin.test.log", previousLog);
        }
    }

    private static String pluginSource() {
        return """
                package testplugins.demo;

                import io.fand.api.command.CommandCompleter;
                import io.fand.api.command.CommandExecutor;
                import io.fand.api.command.CommandSender;
                import io.fand.api.command.CommandSpec;
                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                import io.fand.server.command.AnnotatedCommands;
                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardOpenOption;
                import java.util.List;

                public final class DemoPlugin implements Plugin {
                    @Override
                    public void onEnable(PluginContext context) {
                        context.permissions().register(new io.fand.api.permission.PermissionDescriptor("demo.command.hello", io.fand.api.permission.PermissionDefault.FALSE));
                        context.commands().register(new HelloCommand());
                    }

                    @CommandSpec(label = "hello", subcommands = {"world"}, permission = "demo.command.hello")
                    public static final class HelloCommand implements CommandExecutor, CommandCompleter {
                        @Override
                        public void execute(CommandSender sender, String label, List<String> args) {
                            log("command:" + String.join(",", args));
                        }

                        @Override
                        public List<String> complete(CommandSender sender, String label, List<String> args) {
                            return List.of("alpha", "beta");
                        }
                    }

                    private static void log(String value) {
                        try {
                            Files.writeString(
                                    Path.of(System.getProperty("fand.plugin.test.log")),
                                    value + System.lineSeparator(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND
                            );
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                """;
    }

    private static final class Sender implements io.fand.api.command.CommandSender, io.fand.api.permission.PermissionSubject {

        private final PermissionSet permissions;
        private final List<Component> messages = new ArrayList<>();

        private Sender(boolean allowCommand) {
            this.permissions = new PermissionSet(false);
            if (allowCommand) {
                this.permissions.set("demo.command.hello", true);
            }
        }

        @Override
        public String name() {
            return "sender";
        }

        @Override
        public void sendMessage(Component message) {
            messages.add(message);
        }

        @Override
        public boolean hasPermission(String permission) {
            return permissions.permissionValue(permission).orElse(false);
        }

        @Override
        public boolean operator() {
            return permissions.operator();
        }

        @Override
        public java.util.Optional<Boolean> permissionValue(String node) {
            return permissions.permissionValue(node);
        }
    }
}
