package dev.flashlabs.cratecrate.component.effect;

import dev.flashlabs.cratecrate.component.ValueHolder;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.server.ServerLocation;

public class EffectHolder<K extends Effect<T>, T> extends ValueHolder<K, T> {
    public EffectHolder(K effect, T value) {
        super(effect, value);
    }

    public boolean give(Player player, ServerLocation location) {
        return component.give(player, location, value);
    }
}