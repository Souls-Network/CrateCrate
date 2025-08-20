package dev.flashlabs.cratecrate;

import dev.flashlabs.cratecrate.internal.Serializers;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Optional;

public record DisplayItem(Optional<String> name, List<String> lore, Optional<ItemStackSnapshot> icon) {
    public DisplayItem() {
        this(Optional.empty(), List.of(), Optional.empty());
    }

    public static DisplayItem deserialize(ConfigurationNode node) throws SerializationException {
        var name = Optional.ofNullable(node.node("name").get(String.class));
        var lore = node.node("lore").getList(String.class, List.of());
        var icon = Serializers.ITEM_STACK.deserializeOptional(node.node("icon")).map(ItemStackLike::asImmutable);

        return new DisplayItem(name, lore, icon);
    }
}
