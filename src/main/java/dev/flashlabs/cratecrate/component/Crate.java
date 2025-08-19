package dev.flashlabs.cratecrate.component;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.effect.Effect;
import dev.flashlabs.cratecrate.component.key.Key;
import dev.flashlabs.cratecrate.component.opener.Opener;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import dev.flashlabs.flashlibs.message.MessageTemplate;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.text.WordUtils;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.math.BigDecimal;
import java.util.*;

public final class Crate extends Component<Void> {

    public static final CrateType TYPE = new CrateType();
    public static final Map<String, Type<? extends Crate, ?>> TYPES = new HashMap<>();

    private static final Random RANDOM = new Random();

    private final Optional<String> name;
    private final Optional<List<String>> lore;
    private final Optional<ItemStackSnapshot> icon;
    private final Optional<String> message;
    private final Optional<String> broadcast;
    private final Optional<Opener> opener;
    private final List<Tuple<? extends Key, Integer>> keys;
    private final Map<Effect.Action, List<Tuple<? extends Effect, ?>>> effects;
    private final List<Tuple<Reward, BigDecimal>> rewards;

    private Crate(
        String id,
        Optional<String> name,
        Optional<List<String>> lore,
        Optional<ItemStackSnapshot> icon,
        Optional<String> message,
        Optional<String> broadcast,
        Optional<Opener> opener,
        List<Tuple<? extends Key, Integer>> keys,
        Map<Effect.Action, List<Tuple<? extends Effect, ?>>> effects,
        List<Tuple<Reward, BigDecimal>> rewards
    ) {
        super(id);
        this.name = name;
        this.lore = lore;
        this.icon = icon;
        this.message = message;
        this.broadcast = broadcast;
        this.opener = opener;
        this.keys = keys;
        this.effects = effects;
        this.rewards = rewards;
    }

    /**
     * Returns the name of this crate, defaulting to the capitalized id.
     */
    @Override
    public net.kyori.adventure.text.Component name(Optional<Void> ignored) {
        return name
            .map(s -> LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s))
            .orElseGet(() -> net.kyori.adventure.text.Component.text(WordUtils.capitalize(id), NamedTextColor.WHITE));
    }

    /**
     * Returns the lore of this crate, defaulting to an empty list.
     */
    @Override
    public List<net.kyori.adventure.text.Component> lore(Optional<Void> ignored) {
        return lore.orElseGet(List::of).stream()
            .map(s -> LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s).asComponent())
            .toList();
    }

    /**
     * Returns the icon of this crate, defaulting to a chest. If the icon does
     * not have a defined display name or lore, it is set to this crate's
     * name/lore.
     */
    @Override
    public ItemStack icon(Optional<Void> ignored) {
        var base = icon.map(ItemStackSnapshot::asMutable)
            .orElseGet(() -> ItemStack.of(ItemTypes.CHEST, 1));
        if (base.get(Keys.CUSTOM_NAME).isEmpty()) {
            base.offer(Keys.CUSTOM_NAME, name(Optional.empty()));
        }
        if (base.get(Keys.LORE).isEmpty()) {
            base.offer(Keys.LORE, lore(Optional.empty()));
        }
        return base;
    }

    public List<Tuple<? extends Key, Integer>> keys() {
        return keys;
    }

    public Map<Effect.Action, List<Tuple<? extends Effect, ?>>> effects() {
        return effects;
    }

    public List<Tuple<Reward, BigDecimal>> rewards() {
        return rewards;
    }

    public boolean open(ServerPlayer player, ServerLocation location) {
        effects.get(Effect.Action.OPEN).forEach(e -> e.first().give(player, location, e.second()));
        return opener.map(o -> o.open(player, this, location)).orElseGet(() -> give(player, roll(player), location));
    }

    public boolean give(ServerPlayer player, Tuple<? extends Reward, BigDecimal> reward, ServerLocation location) {
        Optional.ofNullable(reward.first().message().orElse(message.orElse(null)))
                .filter(m -> !m.isEmpty() && !message.orElse("x").isEmpty())
                .ifPresent(m -> player.sendMessage(MessageTemplate.of(m).get(
                        "player", player.name(),
                        "crate", name(Optional.empty()),
                        "reward", reward.first().name(Optional.of(reward.second()))
                )));
        effects.get(Effect.Action.GIVE).forEach(e -> e.first().give(player, location, e.second()));
        return reward.first().give(player.user());
    }

    /**
     * Returns a random reward rolled from this crate. Currently, rewards are
     * not dependent on the player but this is likely to change in the future.
     */
    public Tuple<? extends Reward, BigDecimal> roll(ServerPlayer player) {
        var sum = rewards.stream().map(Tuple::second).reduce(BigDecimal.ZERO, BigDecimal::add);
        var selection = BigDecimal.valueOf(RANDOM.nextDouble()).multiply(sum);
        for (Tuple<? extends Reward, BigDecimal> reward : rewards) {
            selection = selection.subtract(reward.second());
            if (selection.compareTo(BigDecimal.ZERO) <= 0) {
                return reward;
            }
        }
        //TODO: Handle properly for player dependent rewards
        throw new AssertionError("No available rewards.");
    }

    public static final class CrateType extends Type<Crate, Void> {

        private CrateType() {
            super("Crate", CrateCrate.get().getContainer());
        }

        /**
         * Deserializes a crate, defined as:
         *
         * <pre>{@code
         * Reward:
         *     name: Optional<String>
         *     lore: Optional<List<String>>
         *     icon: Optional<ItemStack>
         *     keys: List<KeyReference>
         *     rewards: List<RewardReference>
         * }</pre>
         */
        @Override
        public Crate deserializeComponent(ConfigurationNode node) throws SerializationException {
            var name = Optional.ofNullable(node.node("name").get(String.class));
            var lore = node.node("lore").isList() ? Optional.ofNullable(node.node("lore").getList(String.class)).map(List::copyOf) : Optional.<List<String>>empty();
            var icon = node.hasChild("icon") ? Optional.of(Serializers.ITEM_STACK.deserialize(node.node("icon")).asImmutable()) : Optional.<ItemStackSnapshot>empty();
            var message = Optional.ofNullable(node.node("message").get(String.class));
            var broadcast = Optional.ofNullable(node.node("broadcast").get(String.class));
            var opener = Optional.ofNullable(node.node("opener")).map(Opener::deserialize);

            var keys = new ArrayList<Tuple<? extends Key, Integer>>();
            for (ConfigurationNode key : node.node("keys").childrenList()) {
                var component = key.isList() ? key.node(0) : key;
                var values = key.childrenList().subList(key.isList() ? 1 : 0, key.childrenList().size());
                keys.add(Config.resolveKeyType(component).deserializeReference(component, values));
            }

            Map<Effect.Action, List<Tuple<? extends Effect, ?>>> effects = new HashMap<>();
            for(Effect.Action action : Effect.Action.values()) {
                var effectNode = node.node("effects", action.name().toLowerCase());

                var tuples = new ArrayList<Tuple<? extends Effect, ?>>();
                for (ConfigurationNode effect : effectNode.childrenList()) {
                    var component = effect.isList() ? effect.node(0) : effect;
                    var values = effect.childrenList().subList(effect.isList() ? 1 : 0, effect.childrenList().size());
                    tuples.add(Config.resolveEffectType(component).deserializeReference(component, values));
                }

                effects.put(action, tuples);
            }


            var rewards = new ArrayList<Tuple<Reward, BigDecimal>>();
            for (ConfigurationNode reward : node.node("rewards").childrenList()) {
                var component = reward.isList() ? reward.node(0) : reward;
                var values = reward.childrenList().subList(reward.isList() ? 1 : 0, reward.childrenList().size());
                rewards.add(Config.resolveRewardType(component).deserializeReference(component, values));
            }
            return new Crate(String.valueOf(node.key()), name, lore, icon, message, broadcast, opener, List.copyOf(keys), Map.copyOf(effects), List.copyOf(rewards));
        }

        @Override
        public Tuple<Crate, Void> deserializeReference(ConfigurationNode node, List<? extends ConfigurationNode> values) {
            throw new AssertionError("Crates cannot be referenced.");
        }

    }

}
