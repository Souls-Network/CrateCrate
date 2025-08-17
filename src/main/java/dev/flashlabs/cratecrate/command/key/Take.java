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
import org.spongepowered.api.util.locale.LocaleSource;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public final class Take {

    public static final Component USAGE = CommandUtils.usage(
            "/crate key take ",
            "Takes a key from a user.",
            CommandUtils.argument("user", false, "A username or selector matching a single user (online/offline), defaulting to the player executing this command."),
            CommandUtils.argument("key", true, "A registered key id."),
            CommandUtils.argument("quantity", true, "An integer quantity (> 0).")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.key.take.base")
        .addParameter(Parameter.user().key("user").build())
        .addParameter(Parameter.choices(Key.class, Config.KEYS::get, Config.KEYS::keySet).key("key").build())
        .addParameter(Parameter.rangedInteger(1, Integer.MAX_VALUE).key("quantity").build())
        .executor(Take::execute)
        .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var user = Utils.user(context, "user");
        var key = context.requireOne(Parameter.key("key", Key.class));
        var quantity = context.requireOne(Parameter.key("quantity", Integer.class));
        int balance = key.quantity(user).orElse(0);
        if (quantity <= 0) {
            throw new CommandException(CrateCrate.get().getMessage("command.key.take.invalid-quantity", ((LocaleSource) context.cause().audience()).locale(),
                    "quantity", quantity,
                    "bound", 0
            ));
        } else if (quantity > balance) {
            throw new CommandException(CrateCrate.get().getMessage("command.key.take.excessive-quantity", ((LocaleSource) context.cause().audience()).locale(),
                    "quantity", quantity,
                    "balance", key.quantity(user).orElse(0)
            ));
        }
        if (key.take(user, quantity)) {
            CrateCrate.get().sendMessage((Audience & LocaleSource) context.cause().audience(), "command.key.take.success",
                    "user", user.name(),
                    "key", key.id(),
                    "quantity", quantity,
                    "balance", balance - quantity
            );
        } else {
            throw new CommandException(CrateCrate.get().getMessage("command.key.take.failure", ((LocaleSource) context.cause().audience()).locale(),
                    "user", user.name(),
                    "key", key.id(),
                    "quantity", quantity
            ));
        }
        return CommandResult.success();
    }

}
