package dev.flashlabs.cratecrate.command.location;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.internal.Storage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.util.locale.LocaleSource;
import org.spongepowered.api.world.server.ServerLocation;

import java.sql.SQLException;
import java.util.Optional;

public final class Delete {
    public static final Component USAGE = CommandUtils.usage(
            "/crate location delete ",
            "Deletes a registered crate location.",
            CommandUtils.argument("location", true, "A world (optional for players) and xyz position."),
            CommandUtils.argument("crate", true, "A registered crate id.")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.location.delete.base")
        .addParameter(Parameter.location().key("location").build())
        .executor(Delete::execute)
        .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var location = context.requireOne(Parameter.key("location", ServerLocation.class));
        location = location.withBlockPosition(location.blockPosition());

        if (!Storage.LOCATIONS.containsKey(location)) {
            throw new CommandException(CrateCrate.get().getMessage("command.location.delete.invalid-location", ((LocaleSource) context.cause().audience()).locale(),
                    "location", location.worldKey().toString() + " " + location.position()
            ));
        }
        //TODO: Get registered crate id from database?
        Optional<Crate> crate = Storage.LOCATIONS.get(location);
        try {
            Storage.deleteLocation(location);
            CrateCrate.get().sendMessage((Audience & LocaleSource) context.cause().audience(), "command.location.delete.success",
                    "location", location.worldKey().asString() + " " + location.position(),
                    "crate", crate.map(a -> a.id()).orElse("unavailable")
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CommandException(CrateCrate.get().getMessage("command.location.delete.failure", ((LocaleSource) context.cause().audience()).locale(),
                    "location", location.worldKey().asString() + " " + location.position(),
                    "crate", crate.map(a -> a.id()).orElse("unavailable")
            ));
        }

        return CommandResult.success();
    }

}
