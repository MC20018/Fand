package io.fand.server.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandArgument;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionSubject;
import io.fand.server.permission.PermissionManager;
import io.fand.server.permission.PermissionSet;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class CommandManagerTest {

    @Test
    void resolvesUniqueLocalAndNamespacedCommands() {
        var manager = new CommandManager(new PermissionManager());
        manager.register(descriptor("fand", "reload"), noop(), completer());

        var sender = new TestSender();
        assertThat(manager.lookup("reload")).isPresent();
        assertThat(manager.resolve(sender, List.of("reload"))).isPresent();
        assertThat(manager.resolve(sender, List.of("fand:reload"))).isPresent();
    }

    @Test
    void treatsConflictingLocalRootsAsNamespacedOnly() {
        var manager = new CommandManager(new PermissionManager());
        manager.register(descriptor("fand", "reload"), noop(), completer());
        manager.register(descriptor("tools", "reload"), noop(), completer());

        var sender = new TestSender();
        assertThat(manager.lookup("reload")).isEmpty();
        assertThat(manager.resolve(sender, List.of("reload"))).isEmpty();
        assertThat(manager.resolve(sender, List.of("fand:reload"))).isPresent();
        assertThat(manager.resolve(sender, List.of("tools:reload"))).isPresent();
    }

    @Test
    void respectsPermissionsForResolutionAndSuggestions() {
        var manager = new CommandManager(new PermissionManager());
        manager.register(new CommandDescriptor("fand", "config", List.of("reload"), List.of(), "fand.admin"), noop(), completer());

        var denied = new TestSender();
        var allowed = new TestSender("fand.admin");

        assertThat(manager.resolve(denied, List.of("fand:config", "reload"))).isEmpty();
        assertThat(manager.resolve(allowed, List.of("fand:config", "reload"))).isPresent();
        assertThat(manager.suggestions(denied, List.of("fand:c"))).isEmpty();
        assertThat(manager.suggestions(allowed, List.of("fand:c"))).contains("fand:config");
    }

    @Test
    void resolvesSubcommandsAndUsesSeparateCompleter() throws Exception {
        var manager = new CommandManager(new PermissionManager());
        var executed = new ArrayList<String>();
        manager.register(
                new CommandDescriptor("fand", "config", List.of("reload"), List.of(), null),
                (sender, label, args) -> executed.add(label + ":" + String.join(",", args)),
                (sender, label, args) -> List.of("suggested")
        );

        var sender = new TestSender();
        var resolved = manager.resolve(sender, List.of("config", "reload", "now")).orElseThrow();
        resolved.command().executor().execute(sender, resolved.usedLabel(), List.of("now"));

        assertThat(executed).containsExactly("config:now");
        assertThat(manager.suggestions(sender, List.of("config", "r"))).containsExactly("reload");
        assertThat(manager.suggestions(sender, List.of("fand:config", "r"))).containsExactly("reload");
        assertThat(manager.suggestions(sender, List.of("config", "reload", "n"))).containsExactly("suggested");
        assertThat(manager.suggestions(sender, List.of("fand:config", "reload", "n"))).containsExactly("suggested");
    }

    @Test
    void claimsOnlyCompleteRootsForExecution() {
        var manager = new CommandManager(new PermissionManager());
        manager.register(descriptor("fand", "tps"), noop(), completer());
        manager.register(new CommandDescriptor("fand", "config", List.of("reload"), List.of(), null), noop(), completer());

        assertThat(manager.claims(List.of("tp"))).isFalse();
        assertThat(manager.claims(List.of("tps"))).isTrue();
        assertThat(manager.claims(List.of("fand:t"))).isFalse();
        assertThat(manager.claims(List.of("fand:tps"))).isTrue();
        assertThat(manager.claims(List.of("fand:config", "missing"))).isTrue();
    }

    @Test
    void rejectsInvalidNames() {
        var manager = new CommandManager(new PermissionManager());
        assertThatThrownBy(() -> manager.register(descriptor("Bad Ns", "reload"), noop(), completer()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preservesTypedArgumentsDuringNormalization() {
        var manager = new CommandManager(new PermissionManager());
        manager.register(
                new CommandDescriptor(
                        "FAND",
                        "Hello",
                        List.of("Reload"),
                        List.of("Target"),
                        List.of(CommandArgument.players("Targets").asOptional()),
                        List.of("Alias"),
                        null),
                noop(),
                completer());

        var command = manager.lookup("hello").orElseThrow();
        assertThat(command.descriptor().typedArguments()).hasSize(1);
        var argument = command.descriptor().typedArguments().getFirst();
        assertThat(argument.name()).isEqualTo("targets");
        assertThat(argument.optional()).isTrue();
        assertThat(argument.type()).isEqualTo(io.fand.api.command.CommandArgumentType.PLAYERS);
    }

    private static CommandDescriptor descriptor(String namespace, String label) {
        return new CommandDescriptor(namespace, label, List.of(), List.of(), null);
    }

    private static CommandExecutor noop() {
        return (sender, label, args) -> {
        };
    }

    private static CommandCompleter completer() {
        return (sender, label, args) -> List.of();
    }

    private static final class TestSender implements CommandSender, PermissionSubject {

        private final PermissionSet permissions;

        private TestSender(String... permissions) {
            this.permissions = new PermissionSet(false);
            for (var permission : permissions) {
                this.permissions.set(permission, true);
            }
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public void sendMessage(Component message) {
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
