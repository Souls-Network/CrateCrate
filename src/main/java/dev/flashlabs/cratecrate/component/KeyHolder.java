package dev.flashlabs.cratecrate.component;

import dev.flashlabs.cratecrate.component.key.Key;
import org.spongepowered.api.entity.living.player.User;

public class KeyHolder<T extends Key> extends ValueHolder<T, Integer> {
    public KeyHolder(T component, Integer value) {
        super(component, value);
    }

    public boolean check(User user) {
        return component.check(user, value);
    }

    public boolean take(User user) {
        return component.take(user, value);
    }
}
