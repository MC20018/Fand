package io.fand.api.command;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Static metadata for a registered command.
 *
 * @param namespace command namespace, typically the owning plugin id
 * @param label canonical root command name without namespace
 * @param subcommands literal subcommand path after the root command
 * @param arguments argument names advertised to clients for command help and tab completion
 * @param aliases alternate root command names in the same namespace
 * @param permission optional permission required to execute or complete the command
 */
public record CommandDescriptor(
        String namespace,
        String label,
        List<String> subcommands,
        List<String> arguments,
        List<CommandArgument> typedArguments,
        List<String> aliases,
        @Nullable String permission
) {
    public CommandDescriptor {
        subcommands = List.copyOf(subcommands);
        arguments = List.copyOf(arguments);
        typedArguments = List.copyOf(typedArguments);
        aliases = List.copyOf(aliases);
    }

    public CommandDescriptor(
            String namespace,
            String label,
            List<String> subcommands,
            List<String> arguments,
            List<String> aliases,
            @Nullable String permission
    ) {
        this(namespace, label, subcommands, arguments, arguments.stream()
                .map(CommandArgument::string)
                .toList(), aliases, permission);
    }

    public CommandDescriptor(String namespace, String label, List<String> subcommands, List<String> aliases, @Nullable String permission) {
        this(namespace, label, subcommands, List.of("args"), aliases, permission);
    }

    public CommandDescriptor(String namespace, String label, List<String> aliases) {
        this(namespace, label, List.of(), List.of("args"), aliases, null);
    }
}
