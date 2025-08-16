package dev.flashlabs.cratecrate.command.crate;

import dev.flashlabs.cratecrate.command.Base;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.command.key.Key;
import dev.flashlabs.cratecrate.command.location.Location;
import dev.flashlabs.cratecrate.command.prize.Prize;
import dev.flashlabs.cratecrate.command.reward.Reward;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;

public final class Crate {

    public static final Component USAGE = CommandUtils.usage(
            "/crate crate ",
            "The base command for crates.",
            CommandUtils.argument("...", false, "A crate subcommand (give/list/open)")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.crate.base")
        .addChild(Give.COMMAND, "give")
        .addChild(List.COMMAND, "list")
        .addChild(Open.COMMAND, "open")
            .executor(Crate::execute)
            .build();

    private static CommandResult execute(CommandContext context) {
        CommandUtils.paginate(context.cause().audience(), Crate.USAGE, Give.USAGE, List.USAGE, Open.USAGE);
        return CommandResult.success();
    }


}
