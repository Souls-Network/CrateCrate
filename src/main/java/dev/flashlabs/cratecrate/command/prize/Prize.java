package dev.flashlabs.cratecrate.command.prize;

import dev.flashlabs.cratecrate.command.CommandUtils;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

public final class Prize {
    public static final Component USAGE = CommandUtils.usage(
            "/crate prize ",
            "The base command for prizes.",
            CommandUtils.argument("...", false, "A prize subcommand (give).")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.prize.base")
        .addChild(Give.COMMAND, "give")
            .executor(Prize::execute)
        .build();

    public static CommandResult execute(CommandContext args) throws CommandException {
        CommandUtils.paginate(args.cause().audience(), USAGE, Give.USAGE);
        return CommandResult.success();
    }
}
