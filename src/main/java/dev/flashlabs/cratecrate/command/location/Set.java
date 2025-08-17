package dev.flashlabs.cratecrate.command.location;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Storage;
import io.leangen.geantyref.TypeToken;
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

public final class Set {
    public static final Component USAGE = CommandUtils.usage(
            "/crate location set ",
            "Sets a registered crate location.",
            CommandUtils.argument("location", true, "A world (optional for players) and xyz position."),
            CommandUtils.argument("crate", true, "A registered crate id.")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.location.set.base")
        .addParameter(Parameter.location().key("location").build())
        .addParameter(Parameter.choices(Crate.class, Config.CRATES::get, Config.CRATES::keySet).key("crate").build())
        .executor(Set::execute)
        .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var location = context.requireOne(Parameter.key("location", ServerLocation.class));
        var crate = context.requireOne(Parameter.key("crate", TypeToken.get(Crate.class)));
        location = location.withBlockPosition(location.blockPosition());
        if (Storage.LOCATIONS.containsKey(location)) {
            Optional<Crate> registered = Storage.LOCATIONS.get(location);
            throw new CommandException(CrateCrate.get().getMessage("command.location.set.invalid-location", ((LocaleSource) context.cause().audience()).locale(),
                    "location", location.worldKey().asString() + " " + location.position(),
                    "crate", registered.map(a -> a.id()).orElse("unavailable")
            ));
        }
        try {
            Storage.setLocation(location, crate);
            CrateCrate.get().sendMessage((Audience & LocaleSource) context.cause().audience(), "command.location.set.success",
                    "location", location.worldKey().asString() + " " + location.position(),
                    "crate", crate.id()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CommandException(CrateCrate.get().getMessage("command.location.set.failure", ((LocaleSource) context.cause().audience()).locale(),
                    "location", location.worldKey().asString() + " " + location.position(),
                    "crate", crate.id()
            ));
        }
        return CommandResult.success();
    }

}
