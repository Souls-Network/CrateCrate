package dev.flashlabs.cratecrate.command.key;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.key.Key;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Inventory;
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
    public static Command.Parameterized COMMAND = Command.builder()
            .permission("cratecrate.command.key.list.base")
            .addParameter(Parameter.user().key("user").build())
            .executor(List::execute)
            .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var uuid = context.requireOne(Parameter.key("user", UUID.class));

        User user;

        try {
            user = Sponge.server().userManager().load(uuid).get().orElseThrow(() -> new CommandException(Component.text("Invalid user.")));
        } catch (InterruptedException | ExecutionException e) {
            throw new CommandException(Component.text("Unable to load user."));
        }

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
            throw new CommandException(CrateCrate.get().getMessage("command.crate.list.player-only", ((LocaleSource) audience).locale()));
        }


        return CommandResult.success();
    }

}
