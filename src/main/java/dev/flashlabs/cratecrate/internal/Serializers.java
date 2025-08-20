package dev.flashlabs.cratecrate.internal;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.ResourceKeyed;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.item.FireworkShape;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.*;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Serializers {
    private static final Predicate<String> MINECRAFT_ID = Pattern.compile(
            "^(?:minecraft:)?[a-z0-9_/.-]+$|^(?!minecraft:)[a-z0-9_.-]+:[a-z0-9_/.-]+$"
    ).asPredicate();

    public static <T> T optionalNode(ConfigurationNode node, String name, T base, Function<ConfigurationNode, T>function) {
        var potential = node.node(name);

        return potential.virtual() ? base : function.apply(potential);
    }

    public interface Serializer<T> {

        T deserialize(ConfigurationNode node) throws SerializationException;

        default Optional<T> deserializeOptional(ConfigurationNode node) {
            try {
                return Optional.ofNullable(deserialize(node));
            } catch (SerializationException e) {
                return Optional.empty();
            }
        }

        void reserialize(ConfigurationNode node, T value) throws SerializationException;

    }

    public static final Serializer<Currency> CURRENCY = new Serializer<>() {

        @Override
        public Currency deserialize(ConfigurationNode node) throws SerializationException {
            var id = Optional.ofNullable(node.getString()).orElse("");
            return RegistryTypes.CURRENCY.get().findValue(ResourceKey.resolve(id))
                .orElseThrow(() -> new SerializationException(node, Currency.class, "Unknown currency."));
        }

        @Override
        public void reserialize(ConfigurationNode node, Currency value) throws SerializationException {
            node.set(String.class, value.key(RegistryTypes.CURRENCY).formatted());
        }

    };

    public static final Serializer<ItemType> ITEM_TYPE = new Serializer<>() {

        public ItemType deserialize(ConfigurationNode node) throws SerializationException {
            var id = Optional.ofNullable(node.getString()).orElse("");
            return RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.resolve(id))
                .orElseThrow(() -> new SerializationException(node, ItemType.class, "Unknown item type."));
        }

        public void reserialize(ConfigurationNode node, ItemType value) throws SerializationException {
            node.set(String.class, value.key(RegistryTypes.ITEM_TYPE).formatted());
        }
    };

    public static Serializer<SoundType> SOUND_TYPE = new ResourceKeyedRegistrySerializer<>(RegistryTypes.SOUND_TYPE, SoundType.class, "Unknown sound type.");

    public static Serializer<FireworkShape> FIREWORK_SHAPE = new DefaultedRegistryValueSerializer<>(RegistryTypes.FIREWORK_SHAPE, FireworkShape.class, "Unknown firework shape type.");

    public static Serializer<ParticleType> PARTICLE_TYPE = new DefaultedRegistryValueSerializer<>(RegistryTypes.PARTICLE_TYPE, ParticleType.class, "Unknown particle type.");

    public static record ResourceKeyedRegistrySerializer<T extends ResourceKeyed>(DefaultedRegistryType<T> type, Class<T> typeClass, String errorMesage) implements Serializer<T> {

        @Override
        public T deserialize(ConfigurationNode node) throws SerializationException {
            var id = Optional.ofNullable(node.isMap() ? node.node("type") : node).map(ConfigurationNode::getString).filter(MINECRAFT_ID);

            if (id.isPresent()) {
                return type.get().findValue(ResourceKey.resolve(id.get())).orElseThrow(() -> new SerializationException(node, typeClass, errorMesage));
            } else {
                throw new SerializationException("Doesn't match minecraft identifer pattern.");
            }
        }

        @Override
        public void reserialize(ConfigurationNode node, T value) throws SerializationException {
            node.set(String.class, value.key().formatted());
        }
    }

    public static record DefaultedRegistryValueSerializer<T extends DefaultedRegistryValue>(DefaultedRegistryType<T> type, Class<T> typeClass, String errorMesage) implements Serializer<T> {

        @Override
        public T deserialize(ConfigurationNode node) throws SerializationException {
            var id = Optional.ofNullable(node.getString()).orElse("");
            return type.get().findValue(ResourceKey.resolve(id))
                    .orElseThrow(() -> new SerializationException(node, typeClass, errorMesage));
        }

        @Override
        public void reserialize(ConfigurationNode node, T value) throws SerializationException {
            node.set(String.class, value.key(type).formatted());
        }
    }

    public static final Serializer<ItemStack> ITEM_STACK = new Serializer<>() {

        @Override
        public ItemStack deserialize(ConfigurationNode node) throws SerializationException {
            var builder = ItemStack.builder()
                .itemType(ITEM_TYPE.deserialize(node.isMap() ? node.node("type") : node));
            if (node.hasChild("name")) {
                builder.add(Keys.CUSTOM_NAME, LegacyComponentSerializer.legacyAmpersand().deserialize(node.node("name").getString("")));
            }
            if (node.hasChild("lore")) {
                builder.add(Keys.LORE, node.node("lore").childrenList().stream()
                    .map(n -> LegacyComponentSerializer.legacyAmpersand().deserialize(n.getString("")).asComponent())
                    .toList());
            }
            if (node.hasChild("enchantments")) {
                var enchantments = new ArrayList<Enchantment>();
                for (ConfigurationNode n : node.node("enchantments").childrenList()) {
                    var type = RegistryTypes.ENCHANTMENT_TYPE.get()
                        .findValue(ResourceKey.resolve(Optional.ofNullable(n.node(0).getString()).orElse("")))
                        .orElseThrow(() -> new SerializationException(n.node(0), EnchantmentType.class, "Unknown enchantment type."));
                    var level = n.node(1).getInt(1);
                    enchantments.add(Enchantment.of(type, level));
                }
                builder.add(Keys.APPLIED_ENCHANTMENTS, enchantments);
            }
            //TODO: keys (missing RegistryTypes.KEYS)
            if (node.hasChild("nbt")) {
                builder.fromContainer(builder.build().toContainer().set(DataQuery.of("UnsafeData"), node.node("nbt").raw()));
            }
            if (node.hasChild("quantity")) {
                builder.quantity(node.node("quantity").getInt(1));
            }
            return builder.build();
        }

        @Override
        public void reserialize(ConfigurationNode node, ItemStack value) throws SerializationException {
            ITEM_TYPE.reserialize(value.getKeys().isEmpty() && value.quantity() == 1 ? node : node.node("type"), value.type());
            if (value.get(Keys.CUSTOM_NAME).isPresent()) {
                node.node("name").set(LegacyComponentSerializer.legacyAmpersand().serialize(value.get(Keys.CUSTOM_NAME).get()));
            }
            if (value.get(Keys.LORE).isPresent()) {
                node.node("lore").set(value.get(Keys.LORE).get().stream()
                    .map(t -> LegacyComponentSerializer.legacyAmpersand().serialize(t))
                    .toList());
            }
            if (value.get(Keys.APPLIED_ENCHANTMENTS).isPresent()) {
                node.node("enchantments").set(value.get(Keys.APPLIED_ENCHANTMENTS).get().stream()
                    .map(e -> List.of(e.type().key(RegistryTypes.ENCHANTMENT_TYPE), e.level()))
                    .toList());
            }
            //TODO: keys (missing RegistryTypes.KEYS)
            var nbt = value.toContainer().get(DataQuery.of("UnsafeData"));
            if (nbt.isPresent()) {
                node.node("nbt").set(nbt.get());
            }
            if (value.quantity() != 1) {
                node.node("quantity").set(value.quantity());
            }
        }

    };

    public static final Serializer<org.spongepowered.api.effect.potion.PotionEffect> POTION_TYPE = new Serializer<>() {

        @Override
        public org.spongepowered.api.effect.potion.PotionEffect deserialize(ConfigurationNode node) throws SerializationException {
            String[] split = node.getString().split("/");
            org.spongepowered.api.effect.potion.PotionEffect.Builder builder = org.spongepowered.api.effect.potion.PotionEffect.builder().potionType(Sponge.game().registry(RegistryTypes.POTION_EFFECT_TYPE).findValue(ResourceKey.resolve(split[0].contains(":") ? split[0] : "minecraft:" + split[0])).orElseThrow(() -> new SerializationException(node, null, "Unknown potion type.")));
            if (split.length == 2) {
                try {
                    builder.amplifier(Integer.parseInt(split[1]) - 1);
                } catch (NumberFormatException e) {
                    throw new SerializationException(node, null, "Invalid potion amplifier.");
                }
            }
            return builder.duration(Ticks.of(1L)).build();
        }

        @Override
        public void reserialize(ConfigurationNode node, org.spongepowered.api.effect.potion.PotionEffect value) throws SerializationException {
            String id = value.type().key(RegistryTypes.POTION_EFFECT_TYPE).asString();
            node.set(value.amplifier() != 0 ? id + "/" + (value.amplifier() + 1) : id);
        }
    };

}
