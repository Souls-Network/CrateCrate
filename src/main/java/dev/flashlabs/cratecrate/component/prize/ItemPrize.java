package dev.flashlabs.cratecrate.component.prize;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.DisplayItem;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.component.ValueHolder;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Optional;

public final class ItemPrize extends Prize<Integer> {

    public static final Type<ItemPrize> TYPE = new ItemPrizeType();

    private final ItemStackSnapshot item;

    private ItemPrize(
        String id,
        DisplayItem displayItem,
        ItemStackSnapshot item
    ) {
        super(id, displayItem);
        this.item = item;
    }

    /**
     * Returns the name of this key, defaulting to the translation of the item
     * if no keys are set else the capitalized id without a namespace. If a
     * reference value is given, it is appended to the name in the form
     * {@code (x#)}.
     */
    @Override
    public net.kyori.adventure.text.Component name(Optional<Integer> quantity) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize("8f" + displayItem().name().orElse(id))
            .append(Component.text(quantity.map(q -> " (x" + q + ")").orElse("")));
    }

    /**
     * Returns the lore of this key, defaulting to an empty list. The reference
     * value is currently unused.
     */
    @Override
    public List<net.kyori.adventure.text.Component> lore(Optional<Integer> unused) {
        return displayItem().lore().stream()
            .map(s -> LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s).asComponent())
            .toList();
    }

    /**
     * Returns the icon of this prize, defaulting to the item. If the icon
     * does not have a defined display name or lore, it is set to this prize's
     * name/lore with the quantity if it is larger than the max stack size. The
     * quantity of the icon is set to the given quantity if it is no more
     * than the max stack size, else it is {@code 1}.
     */
    @Override
    public ItemStack icon(Optional<Integer> quantity) {
        var base = displayItem().icon().orElse(item).asMutable();
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
    public boolean give(User user, Integer quantity) {
        var result = user.inventory().offer(ItemStack.builder()
            .fromSnapshot(item)
            .quantity(quantity)
            .build());
        if (result.type() == InventoryTransactionResult.Type.SUCCESS) {
            return true;
        } else {
            CrateCrate.get().logger().error("Failed to give item: " + result.type().name());
            return false;
        }
    }

    private static final class ItemPrizeType extends Type<ItemPrize> {

        private ItemPrizeType() {
            super("Item", CrateCrate.get().getContainer());
        }

        /**
         * Deserializes an item prize, defined as:
         *
         * <pre>{@code
         * CommandPrize:
         *     name: Optional<String>
         *     lore: Optional<List<String>>
         *     icon: Optional<ItemStack>
         *     item: ItemStack
         * }</pre>
         */
        @Override
        public ItemPrize deserializeComponent(String id, ConfigurationNode node) throws SerializationException {
            var displayItem = DisplayItem.deserialize(node);
            var item = Serializers.ITEM_STACK.deserialize(node.node("item")).asImmutable();
            return new ItemPrize(id, displayItem, item);
        }

        /**
         * Deserializes an item prize reference, defined as:
         *
         * <pre>{@code
         * ItemPrizeReference:
         *     node:
         *        ItemPrize |
         *        String (ItemPrize id or ItemType)
         *     values: [
         *        Optional<Integer> (defaults to 1)
         *     ]
         * }</pre>
         */
        @Override
        public ValueHolder<ItemPrize, ?> deserializeReference(ConfigurationNode node) throws SerializationException {
            ItemPrize prize;
            if (node.isMap()) {
                prize = deserializeComponent("ItemPrize@" + node.path(), node);
                Config.PRIZES.put(prize.id, prize);
            } else {
                var identifier = Optional.ofNullable(node.getString()).orElse("");
                if (Config.PRIZES.containsKey(identifier)) {
                    prize = (ItemPrize) Config.PRIZES.get(identifier);
                } else {
                    var item = ItemStack.of(RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.resolve(identifier))
                        .orElseThrow(AssertionError::new)).asImmutable();
                    prize = new ItemPrize(identifier, new DisplayItem(), item);
                    Config.PRIZES.put(prize.id, prize);
                }
            }

            var quantity = node.node("quantity").getInt(1);
            return new PrizeValueHolder<>(prize, quantity);
        }

    }

}
