package io.fand.api.command;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Typed command argument metadata advertised by registered commands.
 *
 * <p>This is an API-safe description of Brigadier-like argument shapes. The
 * runtime may use it to build richer client command trees, while legacy
 * executors still receive the final string tokens through {@link CommandExecutor}.
 */
public record CommandArgument(
        String name,
        CommandArgumentType type,
        boolean optional,
        List<String> suggestions,
        @Nullable Key registry
) {
    public CommandArgument {
        name = requireName(name);
        type = Objects.requireNonNull(type, "type");
        suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions"));
    }

    public static CommandArgument string(String name) {
        return new CommandArgument(name, CommandArgumentType.STRING, false, List.of(), null);
    }

    public static CommandArgument greedyString(String name) {
        return new CommandArgument(name, CommandArgumentType.GREEDY_STRING, false, List.of(), null);
    }

    public static CommandArgument player(String name) {
        return new CommandArgument(name, CommandArgumentType.PLAYER, false, List.of(), null);
    }

    public static CommandArgument players(String name) {
        return new CommandArgument(name, CommandArgumentType.PLAYERS, false, List.of(), null);
    }

    public static CommandArgument location(String name) {
        return new CommandArgument(name, CommandArgumentType.LOCATION, false, List.of(), null);
    }

    public static CommandArgument blockPosition(String name) {
        return new CommandArgument(name, CommandArgumentType.BLOCK_POSITION, false, List.of(), null);
    }

    public static CommandArgument literalEnum(String name, List<String> values) {
        return new CommandArgument(name, CommandArgumentType.ENUM, false, values, null);
    }

    public static CommandArgument registryKey(String name, Key registry) {
        return new CommandArgument(name, CommandArgumentType.REGISTRY_KEY, false, List.of(), registry);
    }

    public CommandArgument asOptional() {
        return new CommandArgument(name, type, true, suggestions, registry);
    }

    private static String requireName(String name) {
        var checked = Objects.requireNonNull(name, "name").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("argument name cannot be blank");
        }
        return checked;
    }
}
