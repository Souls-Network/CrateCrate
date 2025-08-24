package dev.flashlabs.cratecrate.command.crate;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.Reward;
import dev.flashlabs.cratecrate.component.RewardValueHolder;
import dev.flashlabs.cratecrate.internal.Config;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.locale.LocaleSource;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.math.BigDecimal;

public final class Give {
    public static final Component USAGE = CommandUtils.usage(
            "/crate crate give ",
            "Gives a reward to a player as if received through this crate.",
            CommandUtils.argument("player", false, "A username or selector matching a single player, defaulting to the player executing this command."),
            CommandUtils.argument("crate", true, "A registered crate id."),
            CommandUtils.argument("reward", true, "A registered reward id."),
            CommandUtils.argument("position", false, "An xyz position or one of the special values #me (source's position) or #target (source's target block), defaulting to the position of the source executing this command.")
    );

    public static Command.Parameterized COMMAND = Command.builder()
            .permission("cratecrate.command.crate.give.base")
            .addParameter(Parameter.playerOrTarget().key("player").build())
            .addParameter(Parameter.choices(Crate.class, Config.CRATES::get, Config.CRATES::keySet).key("crate").build())
            .addParameter(Parameter.choices(Reward.class, Config.REWARDS::get, Config.REWARDS::keySet).key("reward").build())
            .addParameter(Parameter.vector3d().optional().key("position").build())
            .executor(Give::execute)
            .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var player = context.requireOne(Parameter.key("player", ServerPlayer.class));
        var crate = context.requireOne(Parameter.key("crate", Crate.class));
        var reward = context.requireOne(Parameter.key("reward", Reward.class));
        var position = context.one(Parameter.key("position", Vector3d.class)).orElse(player.blockPosition().toDouble().add(0.5, 0.5, 0.5));
        if (crate.give(player, new RewardValueHolder(reward, BigDecimal.ZERO), player.serverLocation().withPosition(position))) {
            CrateCrate.get().sendMessage((Audience & LocaleSource) context.cause().audience(), "command.crate.give.success",
                    "player", player.name(),
                    "crate", crate.id(),
                    "reward", reward.id(),
                    "position", position.toString());
        } else {
            throw new CommandException(CrateCrate.get().getMessage("command.crate.give.failure", ((LocaleSource) context.cause().audience()).locale(),
                    "player", player.name(),
                    "crate", crate.id(),
                    "reward", reward.id(),
                    "position", position
            ));
        }

        return CommandResult.success();
    }

}
