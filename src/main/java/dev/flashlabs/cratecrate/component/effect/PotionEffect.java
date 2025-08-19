package dev.flashlabs.cratecrate.component.effect;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PotionEffect extends Effect<Integer> {

    public static final Type<PotionEffect, Integer> TYPE = new PotionEffectType();

    private final org.spongepowered.api.effect.potion.PotionEffectType type;
    private final int amplifier;
    private final Optional<Boolean> ambient;
    private final Optional<Boolean> particles;

    private PotionEffect(
        String id,
        org.spongepowered.api.effect.potion.PotionEffectType type,
        int amplifier,
        Optional<Boolean> ambient,
        Optional<Boolean> particles
    ) {
        super(id);
        this.type = type;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
    }

    @Override
    public Component name(Optional<Integer> value) {
        Component level = switch (amplifier) {
            case 0 -> Component.empty();
            case 1 -> Component.text(" II");
            case 2 -> Component.text(" III");
            case 3 -> Component.text(" IV");
            case 4 -> Component.text(" V");
            default -> Component.text(" " + amplifier + 1);
        };
        return LinearComponents.linear(type.asComponent(), level);
    }

    @Override
    public List<Component> lore(Optional<Integer> value) {
        return value
            .map(d -> (Component) Component.text(DurationFormatUtils.formatDurationHMS(d))).map(List::of)
            .orElse(List.of());
    }

    @Override
    public ItemStack icon(Optional<Integer> value) {
        return ItemStack.builder()
            .itemType(ItemTypes.SPLASH_POTION)
            .add(Keys.DISPLAY_NAME, name(value))
            .add(Keys.LORE, lore(value))
            .build();
    }

    @Override
    public boolean give(Player player, ServerLocation location, Integer duration) {
        List<org.spongepowered.api.effect.potion.PotionEffect> effects = player.get(Keys.POTION_EFFECTS).orElseGet(ArrayList::new);
        effects.add(org.spongepowered.api.effect.potion.PotionEffect.builder()
            .potionType(type)
            .amplifier(amplifier)
            .ambient(ambient.orElse(false))
            .showParticles(particles.orElse(true))
            .duration(Ticks.of(20L * duration))
            .build());
        return player.offer(Keys.POTION_EFFECTS, effects).isSuccessful();
    }

    private static final class PotionEffectType extends Type<PotionEffect, Integer> {

        private PotionEffectType() {
            super("Potion", CrateCrate.get().getContainer());
        }

        @Override
        public PotionEffect deserializeComponent(Node node) throws SerializationException {
            org.spongepowered.api.effect.potion.PotionEffect base = node.get("potion").getType() == Node.Type.STRING
                ? node.get("potion", Serializers.POTION_TYPE)
                : node.get("potion.type", Serializers.POTION_TYPE);
            Optional<Boolean> ambient = node.get("potion.ambient", Storm.BOOLEAN.optional());
            Optional<Boolean> particles = node.get("potion.particles", Storm.BOOLEAN.optional());
            return new PotionEffect(String.valueOf(node.getKey()), base.getType(), base.getAmplifier(), ambient, particles);
        }

        @Override
        public void reserializeComponent(Node node, PotionEffect component) throws SerializationException {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public Tuple<PotionEffect, Integer> deserializeReference(ConfigurationNode node, List<? extends ConfigurationNode> values) throws SerializationException {
            PotionEffect effect;
            if (node.isMap()) {
                effect = deserializeComponent(node);
                effect = new PotionEffect("PotionEffect@" + node.path(), effect.type, effect.amplifier, effect.ambient, effect.particles);
                Config.EFFECTS.put(effect.id, effect);
            } else {
                String identifier = node.getget(Storm.STRING);
                if (Config.EFFECTS.containsKey(identifier)) {
                    effect = (PotionEffect) Config.EFFECTS.get(identifier);
                } else {
                    org.spongepowered.api.effect.potion.PotionEffect base = node.get(Serializers.POTION_TYPE);
                    effect = new PotionEffect(identifier, base.getType(), base.getAmplifier(), Optional.empty(), Optional.empty());
                    Config.EFFECTS.put(effect.id, effect);
                }
            }
            if (values.isEmpty() && node.get("duration").getType() == Node.Type.UNDEFINED) {
                throw new SerializationException(node, "Expected a reference value for the duration.");
            }
            int duration = (!values.isEmpty() ? values.get(values.size() - 1) : node.get("duration"))
                .get(Storm.INTEGER.range(Range.atLeast(1)));
            return Tuple.of(effect, duration);
        }

        @Override
        public void reserializeReference(Node node, Tuple<PotionEffect, Integer> reference) throws SerializationException {
            throw new UnsupportedOperationException(); //TODO
        }

    }

}