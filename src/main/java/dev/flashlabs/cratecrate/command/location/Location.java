package dev.flashlabs.cratecrate.command.location;

import dev.flashlabs.cratecrate.command.CommandUtils;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

public final class Location {
    public static final Component USAGE = CommandUtils.usage(
            "/crate location ",
            "The base command for locations.",
            CommandUtils.argument("...", false, "A location subcommand (delete/set).")
    );


    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.location.base")
        .addChild(Set.COMMAND, "set")
        .addChild(Delete.COMMAND, "delete")
            .executor(Location::execute)
        .build();

    public static CommandResult execute(CommandContext args) throws CommandException {
        CommandUtils.paginate(args.cause().audience(), Location.USAGE, Delete.USAGE, Set.USAGE);
        return CommandResult.success();
    }

}
