package dev.flashlabs.cratecrate.component.prize;

import dev.flashlabs.cratecrate.component.ValueHolder;
import org.spongepowered.api.entity.living.player.User;

public class PrizeValueHolder<T extends Prize<K>, K> extends ValueHolder<T, K> {
    public PrizeValueHolder(T component, K value) {
        super(component, value);
    }

    public boolean give(User user) {
        return this.component.give(user, value);
    }
}
