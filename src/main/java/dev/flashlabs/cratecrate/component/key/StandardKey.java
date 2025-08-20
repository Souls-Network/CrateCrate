package dev.flashlabs.cratecrate.component.key;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.DisplayItem;
import dev.flashlabs.cratecrate.component.KeyHolder;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.component.ValueHolder;
import dev.flashlabs.cratecrate.component.effect.EffectHolder;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Storage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.text.WordUtils;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class StandardKey extends Key {

    public static final Type<StandardKey> TYPE = new StandardKeyType();
    private final DisplayItem displayItem;

    private StandardKey(
        String id,
        DisplayItem displayItem
    ) {
        super(id);
        this.displayItem = displayItem;
    }

    /**
     * Returns the name of this key, defaulting to the capitizalized id. If a reference value
     * is given, it is appended to the name in the form {@code (x#)}.
     */
    @Override
    public net.kyori.adventure.text.Component name(Optional<Integer> quantity) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + displayItem.name().orElse(WordUtils.capitalize(id.replace("-", " "))))
            .append(Component.text(quantity.map(q -> " (x" + q + ")").orElse("")));
    }

    /**
     * Returns the lore of this key, defaulting to an empty list. The reference
     * value is currently unused.
     */
    @Override
    public List<net.kyori.adventure.text.Component> lore(Optional<Integer> unused) {
        return displayItem.lore().stream()
            .map(s -> LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s).asComponent())
            .toList();
    }

    /**
     * Returns the icon of this key, defaulting to a tripwire hook. If the icon
     * does not have a defined display name or lore, it is set to this key's
     * name/lore with the quantity if it is larger than the max stack size. The
     * quantity of the icon is set to the given quantity if it is no more
     * than the max stack size, else it is {@code 1}.
     */
    @Override
    public ItemStack icon(Optional<Integer> quantity) {
        var base = displayItem.icon().map(ItemStackSnapshot::asMutable)
            .orElseGet(() -> ItemStack.of(ItemTypes.TRIPWIRE_HOOK, 1));
        if (base.get(Keys.CUSTOM_NAME).isEmpty()) {
            base.offer(Keys.CUSTOM_NAME, name(quantity.filter(q -> q > base.maxStackQuantity())));
        }
        if (base.get(Keys.LORE).isEmpty()) {
            base.offer(Keys.LORE, lore(Optional.empty()));
        }
        base.setQuantity(quantity.filter(q -> q <= base.maxStackQuantity()).orElse(1));
        return base;
    }

    @Override
    public Optional<Integer> quantity(User user) {
        try {
            return Optional.of(Storage.queryKeyQuantity(user, this));
        } catch (SQLException e) {
            CrateCrate.get().logger().error("Error getting key quantity.", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean check(User user, Integer value) {
        return quantity(user).map(i -> i >= value).orElse(false);
    }

    @Override
    public boolean give(User user, Integer value) {
        return update(user, value);
    }

    @Override
    public boolean take(User user, Integer value) {
        return update(user, -value);
    }

    private boolean update(User user, int delta) {
        try {
            Storage.updateKeyQuantity(user, this, delta);
            return true;
        } catch (SQLException e) {
            CrateCrate.get().logger().error("Error getting key quantity.", e);
            return false;
        }
    }

    private static final class StandardKeyType extends Type<StandardKey> {

        private StandardKeyType() {
            super("Standard", CrateCrate.get().getContainer());
        }

        /**
         * Deserializes a standard key, defined as:
         *
         * <pre>{@code
         * StandardKey:
         *     name: Optional<String>
         *     lore: Optional<List<String>>
         *     icon: Optional<ItemStack>
         * }</pre>
         */
        @Override
        public StandardKey deserializeComponent(String id, ConfigurationNode node) throws SerializationException {
            var displayItem = DisplayItem.deserialize(node);

            return new StandardKey(id, displayItem);
        }

        /**
         * Deserializes a standard key reference, defined as:
         *
         * <pre>{@code
         * StandardKeyReference:
         *     node:
         *        StandardKey |
         *        String (StandardKey id or any string)
         *     values: [
         *        Optional<Integer> (defaults to 1)
         *     ]
         * }</pre>
         */
        @Override
        public ValueHolder<StandardKey, ?> deserializeReference(ConfigurationNode node) throws SerializationException {
            StandardKey key;
            if (node.isMap()) {
                key = deserializeComponent("StandardKey@" + node.path(), node);
                Config.KEYS.put(key.id, key);
            } else {
                var identifier = Optional.ofNullable(node.getString()).orElse("");
                if (Config.KEYS.containsKey(identifier)) {
                    key = (StandardKey) Config.KEYS.get(identifier);
                } else {
                    key = new StandardKey(identifier, new DisplayItem());
                    Config.KEYS.put(key.id, key);
                }
            }
            int quantity = node.node("quantity").getInt(1);
            return new KeyHolder<>(key, quantity);
        }

    }

}
