package dev.flashlabs.cratecrate.component.opener;

import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.path.Path;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Optional;

public abstract class Opener {

    //TODO: Allow registration of custom opener types
    public enum Type {
        GUI,
        ROULETTE,
    }

    public abstract boolean open(ServerPlayer player, Crate crate, ServerLocation location);

    public static Opener deserialize(ConfigurationNode node) {
        var type = Optional.ofNullable(node.isMap() ? node.getString() : node.node("type").getString()).map(String::toUpperCase).map(Type::valueOf).orElse(null);

        System.out.println("Sad Rawr: " + node);

        if(type == null) return null;

        return switch (type) {
            case GUI -> GuiOpener.deserialize(node);
            case ROULETTE -> RouletteOpener.deserialize(node);
        };
    }
}
