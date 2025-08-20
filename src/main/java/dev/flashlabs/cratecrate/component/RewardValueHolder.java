package dev.flashlabs.cratecrate.component;

import dev.flashlabs.cratecrate.component.prize.PrizeValueHolder;
import org.spongepowered.api.entity.living.player.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class RewardValueHolder extends ValueHolder<Reward, BigDecimal> {
    public RewardValueHolder(Reward component, BigDecimal value) {
        super(component, value);
    }

    public BigDecimal amount() {
        return value;
    }

    public List<PrizeValueHolder<?, ?>> prizes() {
        return component.prizes();
    }

    public Optional<String> message() {
        return component.message();
    }

    public boolean give(User user) {
        return component.give(user);
    }
}
