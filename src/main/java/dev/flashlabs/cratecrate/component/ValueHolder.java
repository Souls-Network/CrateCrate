package dev.flashlabs.cratecrate.component;

import dev.flashlabs.cratecrate.component.prize.ItemPrize;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class ValueHolder<T extends dev.flashlabs.cratecrate.component.Component<K>, K> {
    public final T component;
    protected final K value;

    public ValueHolder(T component, K value) {
        this.component = component;
        this.value = value;
    }

    public Component name() {
        return component.name(Optional.of(value));
    }

    public ItemStack icon() {
        return component.icon(Optional.of(value));
    }

    public List<Component> lore() {
        return component.lore(Optional.of(value));
    }

    public String id() {
        return component.id();
    }

    public K value() {
        return value;
    }

    public T component() {
        return component;
    }
}
