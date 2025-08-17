package dev.flashlabs.cratecrate.command.key;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.key.Key;
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
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.locale.LocaleSource;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public final class Balance {
    public static final Component USAGE = CommandUtils.usage(
            "/crate key balance ",
            "Checks the balance of a user's keys.",
            CommandUtils.argument("user", false, "A username or selector matching a single user (online/offline), defaulting to the player executing this command."),
            CommandUtils.argument("key", true, "A registered key id.")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.key.balance.base")
        .addParameter(Parameter.user().key("user").build())
        .addParameter(Parameter.choices(Key.class, Config.KEYS::get, Config.KEYS::keySet).key("key").build())
        .executor(Balance::execute)
        .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var key = context.requireOne(Parameter.key("key", Key.class));
        var user = Utils.user(context, "user");

        if (context.cause().root() instanceof ServerPlayer player
                && !player.user().equals(user)
                && !context.hasPermission("cratecrate.command.key.balance.other")) {
            throw new CommandException(CrateCrate.get().getMessage("command.key.balance.other.no-permission", player.locale(),
                    "user", user.name()
            ));
        }

        CrateCrate.get().sendMessage((LocaleSource & Audience) context.cause().audience(), "command.key.balance.success",
                "key", key.name(Optional.of(key.quantity(user).orElse(0)))
        );

        return CommandResult.success();
    }

}
