package io.fand.server.command;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandArgument;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.command.RegisteredCommand;
import io.fand.api.command.ResolvedCommand;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import io.fand.server.permission.PermissionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public final class CommandManager implements CommandRegistry {

    private static final Pattern NAME = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");

    public CommandManager() {
        this(new PermissionManager());
    }

    public CommandManager(PermissionService permissions) {
        this.permissions = permissions;
    }

    private final Object lock = new Object();
    private final PermissionService permissions;
    private volatile Snapshot snapshot = Snapshot.empty();

    @Override
    public CommandRegistration register(Object command) {
        return AnnotatedCommands.register(this, command);
    }

    @Override
    public CommandRegistration register(CommandDescriptor descriptor, CommandExecutor executor, CommandCompleter completer) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(completer, "completer");

        var normalized = normalize(descriptor);
        if (normalized.permission() != null && permissions.lookup(normalized.permission()).isEmpty()) {
            permissions.register(new PermissionDescriptor(normalized.permission(), PermissionDefault.OPERATOR));
        }
        var entry = new Entry(normalized, executor, completer, permissions);
        synchronized (lock) {
            var current = snapshot;
            var keys = pathKeys(normalized);
            for (var key : keys) {
                if (current.namespacedPaths.containsKey(key)) {
                    throw new IllegalStateException("Command path already registered: " + key);
                }
            }
            var namespacedPaths = new LinkedHashMap<>(current.namespacedPaths);
            var localRoots = new LinkedHashMap<>(current.localRoots);
            var uniqueLocalRoots = new LinkedHashMap<>(current.uniqueLocalRoots);
            for (var key : keys) {
                namespacedPaths.put(key, entry);
            }
            for (var root : rootKeys(normalized)) {
                trackLocalRoot(root, entry, localRoots, uniqueLocalRoots);
            }
            snapshot = Snapshot.of(namespacedPaths, localRoots, uniqueLocalRoots);
        }
        return new Registration(this, entry);
    }

    @Override
    public List<RegisteredCommand> visibleCommands(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        var current = snapshot;
        var seen = new LinkedHashSet<RegisteredCommand>();
        for (var entry : current.namespacedPaths.values()) {
            if (entry.allowed(sender)) {
                seen.add(entry);
            }
        }
        return List.copyOf(seen);
    }

    public boolean claims(List<String> tokens) {
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty()) {
            return false;
        }
        var normalizedTokens = normalizeTokens(tokens);
        var first = normalizedTokens.getFirst();
        var current = snapshot;
        if (first.contains(":")) {
            return claimsNamespacedRoot(current, first);
        }
        return current.uniqueLocalRoots.containsKey(first);
    }

    private boolean claimsNamespacedRoot(Snapshot current, String root) {
        for (var key : current.namespacedPaths.keySet()) {
            if (root.equals(pathRoot(key))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<RegisteredCommand> lookup(String name) {
        Objects.requireNonNull(name, "name");
        var normalized = normalizeInput(name);
        var current = snapshot;
        var entry = normalized.contains(":")
                ? current.namespacedPaths.get(normalized)
                : current.uniqueLocalRoots.get(normalized);
        if (entry == null || !entry.active()) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    public Optional<ResolvedCommand> resolve(CommandSender sender, List<String> tokens) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        var normalizedTokens = normalizeTokens(tokens);
        var current = snapshot;
        return normalizedTokens.getFirst().contains(":")
                ? resolveNamespaced(current, sender, normalizedTokens)
                : resolveLocal(current, sender, normalizedTokens);
    }

    public List<String> suggestions(CommandSender sender, List<String> tokens) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(tokens, "tokens");
        var current = snapshot;
        if (tokens.isEmpty()) {
            return rootSuggestions(current, sender, "");
        }
        var normalizedTokens = normalizeTokens(tokens);
        if (normalizedTokens.size() == 1) {
            return rootSuggestions(current, sender, normalizedTokens.getFirst());
        }
        var root = normalizedTokens.getFirst();
        var entry = root.contains(":") ? entryForNamespacedRoot(current, sender, root) : current.uniqueLocalRoots.get(root);
        if (entry == null || !entry.allowed(sender)) {
            return List.of();
        }
        return childOrArgumentSuggestions(sender, entry, root, normalizedTokens.subList(1, normalizedTokens.size()));
    }

    private Optional<ResolvedCommand> resolveNamespaced(Snapshot current, CommandSender sender, List<String> tokens) {
        var first = tokens.getFirst();
        var separator = first.indexOf(':');
        if (separator <= 0 || separator == first.length() - 1) {
            return Optional.empty();
        }
        var namespace = first.substring(0, separator);
        var root = first.substring(separator + 1);
        return resolvePath(current, sender, namespace, root, tokens.subList(1, tokens.size()));
    }

    private Optional<ResolvedCommand> resolveLocal(Snapshot current, CommandSender sender, List<String> tokens) {
        var rootEntry = current.uniqueLocalRoots.get(tokens.getFirst());
        if (rootEntry == null || !rootEntry.allowed(sender)) {
            return Optional.empty();
        }
        return resolvePath(current, sender, rootEntry.descriptor.namespace(), tokens.getFirst(), tokens.subList(1, tokens.size()));
    }

    private Optional<ResolvedCommand> resolvePath(Snapshot current, CommandSender sender, String namespace, String root, List<String> tail) {
        for (int length = tail.size(); length >= 0; length--) {
            var key = toPathKey(namespace, root, tail.subList(0, length));
            var entry = current.namespacedPaths.get(key);
            if (entry != null && entry.allowed(sender)) {
                return Optional.of(new ResolvedCommand(entry, length + 1, root));
            }
        }
        return Optional.empty();
    }

    private @Nullable Entry entryForNamespacedRoot(Snapshot current, CommandSender sender, String token) {
        var separator = token.indexOf(':');
        if (separator <= 0 || separator == token.length() - 1) {
            return null;
        }
        var namespace = token.substring(0, separator);
        var root = token.substring(separator + 1);
        for (var entry : current.namespacedPaths.values()) {
            if (entry.descriptor.namespace().equals(namespace)
                    && CommandManager.rootKeys(entry.descriptor).contains(root)
                    && entry.allowed(sender)) {
                return entry;
            }
        }
        return null;
    }

    private List<String> rootSuggestions(Snapshot current, CommandSender sender, String prefix) {
        var suggestions = new LinkedHashSet<String>();
        for (var entry : current.uniqueLocalRoots.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue().allowed(sender)) {
                suggestions.add(entry.getKey());
            }
        }
        for (var entry : current.namespacedPaths.values()) {
            if (!entry.allowed(sender)) {
                continue;
            }
            for (var root : rootKeys(entry.descriptor)) {
                var namespaced = entry.descriptor.namespace() + ":" + root;
                if (namespaced.startsWith(prefix)) {
                    suggestions.add(namespaced);
                }
            }
        }
        return List.copyOf(suggestions);
    }

    private List<String> childOrArgumentSuggestions(CommandSender sender, Entry entry, String usedRoot, List<String> subTokens) {
        var path = entry.descriptor.subcommands();
        if (subTokens.size() <= path.size()) {
            for (int i = 0; i < subTokens.size() - 1; i++) {
                if (!path.get(i).equals(subTokens.get(i))) {
                    return List.of();
                }
            }
            var index = subTokens.size() - 1;
            var expected = path.get(index);
            var typed = subTokens.get(index);
            return expected.startsWith(typed) ? List.of(expected) : List.of();
        }
        for (int i = 0; i < path.size(); i++) {
            if (!path.get(i).equals(subTokens.get(i))) {
                return List.of();
            }
        }
        try {
            var args = subTokens.subList(path.size(), subTokens.size());
            return List.copyOf(entry.completer.complete(sender, localRoot(usedRoot), args));
        } catch (Exception ex) {
            throw new IllegalStateException("Command completer failed for " + entry.descriptor.namespace() + ":" + entry.descriptor.label(), ex);
        }
    }

    private void unregister(Entry entry) {
        synchronized (lock) {
            if (!entry.active) {
                return;
            }
            var current = snapshot;
            var namespacedPaths = new LinkedHashMap<>(current.namespacedPaths);
            var localRoots = new LinkedHashMap<>(current.localRoots);
            var uniqueLocalRoots = new LinkedHashMap<>(current.uniqueLocalRoots);
            entry.active = false;
            for (var key : pathKeys(entry.descriptor)) {
                namespacedPaths.remove(key);
            }
            for (var root : rootKeys(entry.descriptor)) {
                untrackLocalRoot(root, entry, namespacedPaths, localRoots, uniqueLocalRoots);
            }
            snapshot = Snapshot.of(namespacedPaths, localRoots, uniqueLocalRoots);
        }
    }

    private void trackLocalRoot(
            String root,
            Entry entry,
            LinkedHashMap<String, Integer> localRoots,
            LinkedHashMap<String, Entry> uniqueLocalRoots
    ) {
        var next = localRoots.getOrDefault(root, 0) + 1;
        localRoots.put(root, next);
        if (next == 1) {
            uniqueLocalRoots.put(root, entry);
        } else {
            uniqueLocalRoots.remove(root);
        }
    }

    private void untrackLocalRoot(
            String root,
            Entry removed,
            LinkedHashMap<String, Entry> namespacedPaths,
            LinkedHashMap<String, Integer> localRoots,
            LinkedHashMap<String, Entry> uniqueLocalRoots
    ) {
        var current = localRoots.get(root);
        if (current == null) {
            return;
        }
        if (current == 1) {
            localRoots.remove(root);
            uniqueLocalRoots.remove(root);
            return;
        }
        localRoots.put(root, current - 1);
        Entry survivor = null;
        for (var candidate : namespacedPaths.values()) {
            if (candidate == removed) {
                continue;
            }
            if (rootKeys(candidate.descriptor).contains(root)) {
                survivor = candidate;
                break;
            }
        }
        if (current - 1 == 1 && survivor != null) {
            uniqueLocalRoots.put(root, survivor);
        }
    }

    private record Snapshot(
            Map<String, Entry> namespacedPaths,
            Map<String, Integer> localRoots,
            Map<String, Entry> uniqueLocalRoots
    ) {

        private static Snapshot empty() {
            return of(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        private static Snapshot of(
                LinkedHashMap<String, Entry> namespacedPaths,
                LinkedHashMap<String, Integer> localRoots,
                LinkedHashMap<String, Entry> uniqueLocalRoots
        ) {
            return new Snapshot(
                    Collections.unmodifiableMap(new LinkedHashMap<>(namespacedPaths)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(localRoots)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(uniqueLocalRoots))
            );
        }
    }

    private static List<String> pathKeys(CommandDescriptor descriptor) {
        var keys = new ArrayList<String>(1 + descriptor.aliases().size());
        keys.add(toPathKey(descriptor.namespace(), descriptor.label(), descriptor.subcommands()));
        for (var alias : descriptor.aliases()) {
            keys.add(toPathKey(descriptor.namespace(), alias, descriptor.subcommands()));
        }
        return keys;
    }

    static List<String> rootKeys(CommandDescriptor descriptor) {
        var roots = new ArrayList<String>(1 + descriptor.aliases().size());
        roots.add(descriptor.label());
        roots.addAll(descriptor.aliases());
        return roots;
    }

    private static String toPathKey(String namespace, String root, List<String> subcommands) {
        return subcommands.isEmpty()
                ? namespace + ":" + root
                : namespace + ":" + root + " " + String.join(" ", subcommands);
    }

    private static String localRoot(String usedRoot) {
        var separator = usedRoot.indexOf(':');
        return separator >= 0 ? usedRoot.substring(separator + 1) : usedRoot;
    }

    private static String pathRoot(String pathKey) {
        var separator = pathKey.indexOf(' ');
        return separator < 0 ? pathKey : pathKey.substring(0, separator);
    }

    private static CommandDescriptor normalize(CommandDescriptor descriptor) {
        var namespace = normalizePart(descriptor.namespace(), "namespace");
        var label = normalizePart(descriptor.label(), "label");
        var subcommands = new ArrayList<String>(descriptor.subcommands().size());
        for (var subcommand : descriptor.subcommands()) {
            subcommands.add(normalizePart(subcommand, "subcommand"));
        }
        var arguments = new ArrayList<String>(descriptor.arguments().size());
        for (var argument : descriptor.arguments()) {
            arguments.add(normalizePart(argument, "argument"));
        }
        var typedArguments = new ArrayList<CommandArgument>(descriptor.typedArguments().size());
        for (var argument : descriptor.typedArguments()) {
            typedArguments.add(normalizeArgument(argument));
        }
        var aliases = new ArrayList<String>(descriptor.aliases().size());
        for (var alias : descriptor.aliases()) {
            var normalized = normalizePart(alias, "alias");
            if (normalized.equals(label)) {
                continue;
            }
            if (!aliases.contains(normalized)) {
                aliases.add(normalized);
            }
        }
        var permission = descriptor.permission() == null ? null : descriptor.permission().trim();
        return new CommandDescriptor(
                namespace,
                label,
                subcommands,
                arguments,
                typedArguments,
                aliases,
                permission == null || permission.isEmpty() ? null : permission);
    }

    private static CommandArgument normalizeArgument(CommandArgument argument) {
        Objects.requireNonNull(argument, "argument");
        var suggestions = new ArrayList<String>(argument.suggestions().size());
        for (var suggestion : argument.suggestions()) {
            suggestions.add(normalizeInput(suggestion));
        }
        return new CommandArgument(
                normalizePart(argument.name(), "argument"),
                argument.type(),
                argument.optional(),
                suggestions,
                argument.registry());
    }

    private static List<String> normalizeTokens(List<String> tokens) {
        var normalized = new ArrayList<String>(tokens.size());
        for (var token : tokens) {
            normalized.add(normalizeInput(token));
        }
        return normalized;
    }

    private static String normalizeInput(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePart(String value, String role) {
        Objects.requireNonNull(value, role);
        var normalized = normalizeInput(value);
        if (!NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid command " + role + ": " + value);
        }
        return normalized;
    }

    private static final class Registration implements CommandRegistration {

        private final CommandManager owner;
        private final Entry entry;

        private Registration(CommandManager owner, Entry entry) {
            this.owner = owner;
            this.entry = entry;
        }

        @Override
        public boolean active() {
            return entry.active;
        }

        @Override
        public void unregister() {
            owner.unregister(entry);
        }
    }

    public static final class Entry implements RegisteredCommand {

        private final CommandDescriptor descriptor;
        private final CommandExecutor executor;
        private final CommandCompleter completer;
        private final PermissionService permissions;
        private volatile boolean active = true;

        private Entry(CommandDescriptor descriptor, CommandExecutor executor, CommandCompleter completer, PermissionService permissions) {
            this.descriptor = descriptor;
            this.executor = executor;
            this.completer = completer;
            this.permissions = permissions;
        }

        boolean allowed(CommandSender sender) {
            if (!active) {
                return false;
            }
            if (descriptor.permission() == null) {
                return true;
            }
            if (sender instanceof PermissionSubject subject) {
                return permissions.hasPermission(subject, descriptor.permission());
            }
            return sender.hasPermission(descriptor.permission());
        }

        boolean active() {
            return active;
        }

        @Override
        public CommandDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public CommandExecutor executor() {
            return executor;
        }

        @Override
        public CommandCompleter completer() {
            return completer;
        }
    }
}
