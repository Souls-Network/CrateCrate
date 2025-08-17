package dev.flashlabs.cratecrate.command.reward;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.Reward;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Utils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.util.locale.LocaleSource;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public final class Give {
    public static final Component USAGE = CommandUtils.usage(
            "/crate prize give ",
            "Gives a reward to a user.",
            CommandUtils.argument("user", false, "A username or selector matching a single user (online/offline), defaulting to the player executing this command."),
            CommandUtils.argument("reward", true, "A registered reward id.")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.reward.give.base")
        .addParameter(Parameter.user().key("user").build())
        .addParameter(Parameter.choices(Reward.class, Config.REWARDS::get, Config.REWARDS::keySet).key("reward").build())
        .executor(Give::execute)
        .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var user = Utils.user(context,"user");
        var reward = context.requireOne(Parameter.key("reward", Reward.class));

        if (reward.give(user)) {
            CrateCrate.get().sendMessage((Audience & LocaleSource) context.cause().audience(), "command.prize.give.success",
                    "user", user.name(),
                    "reward", reward.id()
            );
        } else {
            throw new CommandException(CrateCrate.get().getMessage("command.prize.give.failure", ((LocaleSource) context.cause().audience()).locale(),
                    "user", user.name(),
                    "reward", reward.id()
            ));
        }
        return CommandResult.success();
    }

}
