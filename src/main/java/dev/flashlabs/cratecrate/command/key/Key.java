package dev.flashlabs.cratecrate.command.key;

import dev.flashlabs.cratecrate.command.CommandUtils;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

public final class Key {

    public static final Component USAGE = CommandUtils.usage(
            "/crate key ",
            "The base command for keys.",
            CommandUtils.argument("...", false, "A key subcommand (balance/give/list/take).")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.key.base")
        .addChild(Balance.COMMAND, "balance")
        .addChild(Give.COMMAND, "give")
        .addChild(Take.COMMAND, "take")
        .addChild(List.COMMAND, "list")
        .executor(Key::execute)
        .build();

    public static CommandResult execute(CommandContext args) throws CommandException {
        CommandUtils.paginate(args.cause().audience(), Key.USAGE, Balance.USAGE, Give.USAGE, List.USAGE, Take.USAGE);
        return CommandResult.success();
    }
}
