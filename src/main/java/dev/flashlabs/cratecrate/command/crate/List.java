package dev.flashlabs.cratecrate.command.crate;

import dev.flashlabs.cratecrate.CrateCrate;
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
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.locale.LocaleSource;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class List {
    public static Command.Parameterized COMMAND = Command.builder()
            .permission("cratecrate.command.crate.list.base")
            .executor(List::execute)
            .build();

    private static CommandResult execute(CommandContext context) throws CommandException {

        if (context.cause().audience() instanceof ServerPlayer player) {
            //TODO: Filter with permission
            Inventory.page(
                    Component.text("Available Crates"),
                    Config.CRATES.values().stream()
                            .map(c -> Element.of(c.icon(Optional.empty()), a -> a.callback(v -> {
                                Utils.preview(c, Element.of(Inventory.item(ItemTypes.CHEST.get(), Component.text("Available Crates")), a2 -> a2.callback(v2 -> {
                                    v.open(player);
                                }))).open(a.getPlayer());
                            })))
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
