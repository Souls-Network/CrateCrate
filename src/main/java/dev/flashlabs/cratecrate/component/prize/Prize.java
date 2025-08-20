package dev.flashlabs.cratecrate.component.prize;

import dev.flashlabs.cratecrate.DisplayItem;
import dev.flashlabs.cratecrate.component.Component;
import dev.flashlabs.cratecrate.component.Type;
import org.spongepowered.api.entity.living.player.User;

import java.util.HashMap;
import java.util.Map;

public abstract class Prize<T> extends Component<T> {

    public static final Map<String, Type<? extends Prize>> TYPES = new HashMap<>();
    private final DisplayItem displayItem;

    protected Prize(String id, DisplayItem displayItem) {
        super(id);
        this.displayItem = displayItem;
    }

    public abstract boolean give(User user, T value);

    public DisplayItem displayItem() {
        return displayItem;
    }
}
