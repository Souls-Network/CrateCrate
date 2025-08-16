package dev.flashlabs.cratecrate.command.crate;

import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.internal.Config;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;

public final class Open {
    public static final Component USAGE = CommandUtils.usage(
            "/crate crate open ",
            "Gives a reward to a player as if given through this crate.",
            CommandUtils.argument("player", false, "A username or selector matching a single player, defaulting to the player executing this command."),
            CommandUtils.argument("crate", true, "A registered crate id."),
            CommandUtils.argument("position", false, "An xyz position or one of the special values #me (source's position) or #target (source's target block), defaulting to the position of the source executing this command.")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.crate.open.base")
        .addParameter(Parameter.player().key("player").build())
        .addParameter(Parameter.choices(Crate.class, Config.CRATES::get, Config.CRATES::keySet).key("crate").build())
        .addParameter(Parameter.location().optional().key("location").build())
        .executor(Open::execute)
        .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var player = context.requireOne(Parameter.key("player", ServerPlayer.class));
        var crate = context.requireOne(Parameter.key("crate", Crate.class));
        var location = context.one(Parameter.key("location", ServerLocation.class));
        if (crate.open(player, location.orElseGet(player::serverLocation))) {
            context.sendMessage(Identity.nil(), Component.text("Successfully opened crate."));
        } else {
            throw new CommandException(Component.text("Failed to open crate."));
        }
        return CommandResult.success();
    }

}
