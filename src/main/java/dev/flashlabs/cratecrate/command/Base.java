package dev.flashlabs.cratecrate.command;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.crate.Crate;
import dev.flashlabs.cratecrate.command.key.Key;
import dev.flashlabs.cratecrate.command.location.Location;
import dev.flashlabs.cratecrate.command.prize.Prize;
import dev.flashlabs.cratecrate.command.reward.Reward;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;

import java.util.List;

public final class Base {
    public static final Component USAGE = CommandUtils.usage(
            "/crate ",
            "The base command for CrateCrate.",
            CommandUtils.argument("...", false, "A subcommand (crate/key/location/prize/reward).")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.base")
        .addChild(Crate.COMMAND, "crate")
        .addChild(Reward.COMMAND, "reward")
        .addChild(Prize.COMMAND, "prize")
        .addChild(Key.COMMAND, "key")
        .addChild(Location.COMMAND, "location")
        .executor(Base::execute)
        .build();

    private static CommandResult execute(CommandContext context) {
        CommandUtils.paginate(context.cause().audience(), Base.USAGE, Crate.USAGE, Key.USAGE, Location.USAGE, Prize.USAGE, Reward.USAGE);
        return CommandResult.success();
    }

}
