package dev.flashlabs.cratecrate.command.key;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.key.Key;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Inventory;
import dev.flashlabs.cratecrate.internal.Utils;
import dev.flashlabs.flashlibs.inventory.Element;
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

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class List {
    public static final Component USAGE = CommandUtils.usage(
            "/crate key list ",
            "Lists all of a user's keys.",
            CommandUtils.argument("user", false, "A username or selector matching a single user (online/offline), defaulting to the player executing this command."),
            CommandUtils.argument("--text", false, "List keys through text rather than GUI (always enabled for console).")
    );

    public static Command.Parameterized COMMAND = Command.builder()
            .permission("cratecrate.command.key.list.base")
            .addParameter(Parameter.user().key("user").build())
            .executor(List::execute)
            .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var user = Utils.user(context, "user");

        var audience = context.cause().audience();

        if (audience instanceof ServerPlayer player) {
            Inventory.page(
                    Component.text(user.name() + "'s Keys"),
                    Config.KEYS.values().stream()
                            .map(c -> Element.of(c.icon(c.quantity(user))))
                            .filter(e -> e.getItem().quantity() > 0)
                            .collect(Collectors.toList()),
                    Inventory.CLOSE
            ).open(player);
        } else {
            //TODO
            throw new CommandException(CrateCrate.get().getMessage("command.key.list.other.no-permission", ((LocaleSource) context.cause().audience()).locale(),
                "user", user.name()));
        }


        return CommandResult.success();
    }

}
