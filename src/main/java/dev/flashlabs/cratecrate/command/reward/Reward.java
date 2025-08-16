package dev.flashlabs.cratecrate.command.reward;

import dev.flashlabs.cratecrate.command.CommandUtils;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

public final class Reward {
    public static final Component USAGE = CommandUtils.usage(
            "/crate reward ",
            "The base command for rewards.",
            CommandUtils.argument("...", false, "A reward subcommand (give).")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.reward.base")
        .addChild(Give.COMMAND, "give")
        .build();

    public static CommandResult execute(CommandContext args) throws CommandException {
        CommandUtils.paginate(args.cause().audience(), Reward.USAGE, Give.USAGE);
        return CommandResult.success();
    }

}
