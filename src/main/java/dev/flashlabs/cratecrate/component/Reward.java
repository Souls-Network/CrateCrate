package dev.flashlabs.cratecrate.component;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.prize.PrizeValueHolder;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.text.WordUtils;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Reward extends Component<BigDecimal> {

    public static final RewardType TYPE = new RewardType();
    public static final Map<String, Type<? extends Reward>> TYPES = new HashMap<>();

    private final Optional<String> name;
    private final Optional<List<String>> lore;
    private final Optional<ItemStackSnapshot> icon;
    private final Optional<String> message;
    private final Optional<String> broadcast;
    private final List<PrizeValueHolder<?, ?>> prizes;

    private Reward(
        String id,
        Optional<String> name,
        Optional<List<String>> lore,
        Optional<ItemStackSnapshot> icon,
        Optional<String> message,
        Optional<String> broadcast,
        List<PrizeValueHolder<?, ?>> prizes
    ) {
        super(id);
        this.name = name;
        this.lore = lore;
        this.icon = icon;
        this.message = message;
        this.broadcast = broadcast;
        this.prizes = prizes;
    }

    /**
     * Returns the name of this reward, defaulting to either the name of the
     * first prize (if only one prize exists) or this reward's capitalized id (if multiple
     * prizes exist). The reference value is currently unused.
     */
    @Override
    public net.kyori.adventure.text.Component name(Optional<BigDecimal> unused) {
        if (name.isPresent()) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + name.get());
        } else if (prizes.size() == 1) {
            return prizes.get(0).name();
        } else {
            return net.kyori.adventure.text.Component.text(WordUtils.capitalize(id.replace("-", " ")), NamedTextColor.WHITE);
        }
    }

    /**
     * Returns the lore of this reward, defaulting to either the lore of the
     * first prize (if only one prize exists) or the names of all prizes (if
     * multiple prizes exist). If a reference value is provided, it replaces
     * {@code ${weight}}.
     */
    @Override
    public List<net.kyori.adventure.text.Component> lore(Optional<BigDecimal> weight) {
        if (lore.isPresent()) {
            return lore.get().stream().map(s -> {
                s = s.replaceAll("\\$\\{weight}", weight.map(String::valueOf).orElse("${weight}"));
                return LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s).asComponent();
            }).toList();
        } else if (prizes.size() == 1) {
            return prizes.get(0).lore();
        } else {
            return prizes.stream().map(ValueHolder::name).toList();
        }
    }

    /**
     * Returns the icon of this reward, defaulting to either the icon of the
     * first prize (if only one prize exists) or a book (if multiple prizes
     * exist). If the icon does not have a defined display name or lore, it is
     * set to this reward's name/lore.
     */
    @Override
    public ItemStack icon(Optional<BigDecimal> weight) {
        var base = icon.map(ItemStackSnapshot::asMutable).orElseGet(() -> {
            if (prizes.size() == 1) {
                return prizes.get(0).icon();
            } else {
                return ItemStack.of(ItemTypes.BOOK, 1);
            }
        });
        if (base.get(Keys.CUSTOM_NAME).isEmpty()) {
            base.offer(Keys.CUSTOM_NAME, name(weight));
        }
        if (base.get(Keys.LORE).isEmpty()) {
            base.offer(Keys.LORE, lore(weight));
        }
        return base;
    }


    public Optional<String> message() {
        return message;
    }

    public Optional<String> broadcast() {
        return broadcast;
    }


    public List<PrizeValueHolder<?, ?>> prizes() {
        return prizes;
    }

    public boolean give(User user) {
        return prizes.stream().allMatch(p -> p.give(user));
    }

    public static final class RewardType extends Type<Reward> {

        public RewardType() {
            super("Reward", CrateCrate.get().getContainer());
        }

        /**
         * Deserializes a reward, defined as:
         *
         * <pre>{@code
         * Reward:
         *     name: Optional<String>
         *     lore: Optional<List<String>>
         *     icon: Optional<ItemStack>
         *     prizes: List<PrizeReference>
         * }</pre>
         */
        @Override
        public Reward deserializeComponent(String id, ConfigurationNode node) throws SerializationException {
            var name = Optional.ofNullable(node.node("name").get(String.class));
            var lore = Optional.ofNullable(node.node("lore").getList(String.class)).map(List::copyOf);
            var icon = Optional.ofNullable(Serializers.ITEM_STACK.deserialize(node.node("icon"))).map(ItemStackLike::asImmutable);
            var message = Optional.ofNullable(node.node("message").get(String.class));
            var broadcast = Optional.ofNullable(node.node("broadcast").get(String.class));

            var prizes = new ArrayList<PrizeValueHolder<?, ?>>();
            for (ConfigurationNode prize : node.node("prizes").childrenList()) {
                var type = Config.resolvePrizeType(prize);

                System.out.println("Type: " + type.name());

                prizes.add((PrizeValueHolder<?, ?>) Config.resolvePrizeType(prize).deserializeReference(prize));
            }

            return new Reward(id, name, lore, icon, message, broadcast, List.copyOf(prizes));
        }

        /**
         * Deserializes a reward reference, defined as:
         *
         * <pre>{@code
         * RewardReference:
         *     node: Reward | String (Reward id) | PrizeReference (map/string)
         *        weight: BigDecimal (required for Reward, required for
         *            PrizeReference when reference value is not defined)
         *     values: [
         *        list... (limit 1 for Reward/String),
         *        Optional<BigDecimal> (required for String, required for
         *            PrizeReference when weight is not defined)
         *     ]
         * }</pre>
         */
        @Override
        public RewardValueHolder deserializeReference(ConfigurationNode node) throws SerializationException {
            Reward reward;
            if (node.isMap()) {
                if (node.hasChild("prizes")) {
                    reward = deserializeComponent("Reward@" + node.path(), node);

                    Config.REWARDS.put(reward.id, reward);
                } else {
                    var identifier = Optional.ofNullable(node.getString()).orElse("");
                    if (Config.REWARDS.containsKey(identifier)) {
                        reward = Config.REWARDS.get(identifier);
                    } else {
                        var prize = (PrizeValueHolder<?, ?>) Config.resolvePrizeType(node).deserializeReference(node);
                        reward = new Reward(identifier, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(prize));
                        Config.REWARDS.put(reward.id, reward);
                    }
                }
            } else {
                throw new AssertionError();
            }

            var value = new BigDecimal(node.node("weight").getString("0"));
            return new RewardValueHolder(reward, value);
        }

        @Override
        public boolean matches(ConfigurationNode node) {
            return true;
        }
    }

}
