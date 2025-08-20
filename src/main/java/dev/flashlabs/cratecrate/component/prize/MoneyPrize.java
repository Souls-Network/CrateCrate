package dev.flashlabs.cratecrate.component.prize;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.DisplayItem;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.component.ValueHolder;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public final class MoneyPrize extends Prize<BigDecimal> {

    public static final Type<MoneyPrize> TYPE = new MoneyPrizeType();

    private final Optional<Currency> currency;

    private MoneyPrize(
        String id,
        DisplayItem displayItem,
        Optional<Currency> currency
    ) {
        super(id, displayItem);
        this.currency = currency;
    }

    /**
     * Returns the name of this prize, defaulting to the format method of the
     * currency. If a reference value is given, it replaces {@code ${amount}}.
     */
    @Override
    public Component name(Optional<BigDecimal> amount) {
        return displayItem().name().map(s -> {
            s = s.replaceAll("\\$\\{amount}", amount.map(String::valueOf).orElse("${amount}"));
            return LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s).asComponent();
        }).orElseGet(() -> {
            var service = Sponge.server().serviceProvider().economyService().get();
            return currency
                    .orElse(service.defaultCurrency())
                    .format(amount.orElse(BigDecimal.ZERO)).color(NamedTextColor.WHITE);
//                    .replace(Pattern.compile("0+(\\.0+)"), Text.of("${amount}"))
        });
    }

    /**
     * Returns the lore of this prize, defaulting to an empty list. If a
     * reference value is given, it replaces {@code ${amount}}.
     */
    @Override
    public List<Component> lore(Optional<BigDecimal> amount) {
        return displayItem().lore().stream().map(s -> {
            s = s.replaceAll("\\$\\{amount}", amount.map(String::valueOf).orElse("${amount}"));
            return LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s).asComponent();
        }).toList();
    }

    /**
     * Returns the icon of this prize, defaulting to a sunflower. If the icon
     * does not have a defined display name or lore, it is set to this prize's
     * name/lore.
     */
    @Override
    public ItemStack icon(Optional<BigDecimal> value) {
        var base = displayItem().icon().map(ItemStackSnapshot::asMutable)
            .orElseGet(() -> ItemStack.of(ItemTypes.SUNFLOWER, 1));
        if (base.get(Keys.CUSTOM_NAME).isEmpty()) {
            base.offer(Keys.CUSTOM_NAME, name(value));
        }
        if (base.get(Keys.LORE).isEmpty()) {
            base.offer(Keys.LORE, lore(value));
        }
        return base;
    }

    @Override
    public boolean give(User user, BigDecimal amount) {
        var service = Sponge.server().serviceProvider().economyService().get();
        var account = service.findOrCreateAccount(user.uniqueId()).orElse(null);
        if (account != null) {
            var result = account.deposit(currency.orElse(service.defaultCurrency()), amount);
            return result.result() == ResultType.SUCCESS;
        } else {
            return false;
        }
    }

    private static final class MoneyPrizeType extends Type<MoneyPrize> {

        private MoneyPrizeType() {
            super("Money", CrateCrate.get().getContainer());
        }

        /**
         * Deserializes a money prize, defined as:
         *
         * <pre>{@code
         * MoneyPrize:
         *     name: Optional<String>
         *     lore: Optional<List<String>>
         *     icon: Optional<ItemStack>
         *     money: Object
         *         currency: String (a registered currency)
         * }</pre>
         */
        @Override
        public MoneyPrize deserializeComponent(String id, ConfigurationNode node) throws SerializationException {
            var currency = node.hasChild("money", "currency")
                ? Optional.of(Serializers.CURRENCY.deserialize(node.node("money", "currency")))
                : Optional.<Currency>empty();
            return new MoneyPrize(id, DisplayItem.deserialize(node), currency);
        }

        /**
         * Deserializes a money prize reference, defined as:
         *
         * <pre>{@code
         * MoneyPrizeReference:
         *     node:
         *        MoneyPrize |
         *        String (MoneyPrize id or prefixed with '$')
         *     values: [
         *        Optional<Decimal> (only allowed with String MoneyPrize id)
         *     ]
         * }</pre>
         */
        @Override
        public ValueHolder<MoneyPrize, BigDecimal> deserializeReference(ConfigurationNode node) throws SerializationException {
            MoneyPrize prize;
            if (node.isMap()) {
                prize = deserializeComponent("MoneyPrize@" + node.path(), node);
                Config.PRIZES.put(prize.id, prize);
            } else {
                var identifier = node.getString("");
                if (Config.PRIZES.containsKey(identifier)) {
                    prize = (MoneyPrize) Config.PRIZES.get(identifier);
                } else if (identifier.matches("\\$[0-9]+(\\.[0-9]+)?")) {
                    prize = (MoneyPrize) Config.PRIZES.computeIfAbsent("$", k -> new MoneyPrize(k, new DisplayItem(), Optional.empty()));
                    return new PrizeValueHolder<>(prize, new BigDecimal(identifier.substring(1)));
                } else if (identifier.startsWith("$")) {
                    //TODO: Currency registry is empty and not accessible via EconomyService
                    var currency = RegistryTypes.CURRENCY.get().findValue(ResourceKey.resolve(identifier.substring(1))).get();
                    prize = new MoneyPrize(identifier, new DisplayItem(), Optional.of(currency));
                    Config.PRIZES.put(prize.id, prize);
                } else {
                    throw new AssertionError(identifier);
                }
            }
            //TODO: Validate reference value counts
            var amount = new BigDecimal(node.node("amount").getString("0"));
            return new PrizeValueHolder<>(prize, amount);
        }

    }

}
