package io.fand.server.plugin;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.command.RegisteredCommand;
import io.fand.api.command.ResolvedCommand;
import io.fand.server.command.AnnotatedCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PluginCommandRegistry implements CommandRegistry {

    private final CommandRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginCommandRegistry(CommandRegistry delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = delegate;
        this.tracker = tracker;
        this.namespace = namespace;
    }

    public String namespace() {
        return namespace;
    }

    @Override
    public CommandRegistration register(Object command) {
        return AnnotatedCommands.register(this, command);
    }

    @Override
    public CommandRegistration register(CommandDescriptor descriptor, CommandExecutor executor, CommandCompleter completer) {
        var scoped = new CommandDescriptor(
                namespace,
                descriptor.label(),
                descriptor.subcommands(),
                descriptor.arguments(),
                descriptor.typedArguments(),
                descriptor.aliases(),
                descriptor.permission()
        );
        return tracker.track(delegate.register(scoped, executor, completer), scoped);
    }

    @Override
    public Optional<RegisteredCommand> lookup(String name) {
        return delegate.lookup(name).filter(this::ownedByThisPlugin);
    }

    @Override
    public boolean claims(List<String> tokens) {
        return !tokens.isEmpty() && lookup(tokens.getFirst()).isPresent();
    }

    @Override
    public Optional<ResolvedCommand> resolve(CommandSender sender, List<String> tokens) {
        return delegate.resolve(sender, tokens)
                .filter(resolved -> ownedByThisPlugin(resolved.command()));
    }

    @Override
    public List<String> suggestions(CommandSender sender, List<String> tokens) {
        if (tokens.size() > 1 && lookup(tokens.getFirst()).isEmpty()) {
            return List.of();
        }
        var suggestions = delegate.suggestions(sender, tokens);
        if (tokens.size() > 1) {
            return suggestions;
        }
        return suggestions.stream()
                .filter(suggestion -> lookup(suggestion).isPresent())
                .toList();
    }

    @Override
    public List<RegisteredCommand> visibleCommands(CommandSender sender) {
        var filtered = new ArrayList<RegisteredCommand>();
        for (var command : delegate.visibleCommands(sender)) {
            if (ownedByThisPlugin(command)) {
                filtered.add(command);
            }
        }
        return List.copyOf(filtered);
    }

    private boolean ownedByThisPlugin(RegisteredCommand command) {
        return namespace.equals(command.descriptor().namespace());
    }
}
