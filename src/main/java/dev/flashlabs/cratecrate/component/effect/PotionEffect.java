package dev.flashlabs.cratecrate.component.effect;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.component.ValueHolder;
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
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PotionEffect extends Effect<Integer> {

    public static final Type<PotionEffect> TYPE = new PotionEffectType();

    private final org.spongepowered.api.effect.potion.PotionEffectType type;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;

    private PotionEffect(
        String id,
        org.spongepowered.api.effect.potion.PotionEffectType type,
        int amplifier,
        boolean ambient,
        boolean particles
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
            .ambient(ambient)
            .showParticles(particles)
            .duration(Ticks.of(20L * duration))
            .build());
        return player.offer(Keys.POTION_EFFECTS, effects).isSuccessful();
    }

    private static final class PotionEffectType extends Type<PotionEffect> {

        private PotionEffectType() {
            super("Potion", CrateCrate.get().getContainer());
        }

        @Override
        public PotionEffect deserializeComponent(String id, ConfigurationNode node) throws SerializationException {
            org.spongepowered.api.effect.potion.PotionEffect base = Serializers.POTION_TYPE.deserialize(node.node("potion"));
            boolean ambient = node.node("potion.ambient").getBoolean(false);
            boolean particles = node.node("potion.particles").getBoolean(true);
            return new PotionEffect(id, base.type(), base.amplifier(), ambient, particles);
        }

        @Override
        public ValueHolder<PotionEffect, ?> deserializeReference(ConfigurationNode node) throws SerializationException {
            PotionEffect effect;
            if (node.isMap()) {
                effect = deserializeComponent("PotionEffect@" + node.path(), node);
                Config.EFFECTS.put(effect.id, effect);
            } else {
                String identifier = node.getString("");
                if (Config.EFFECTS.containsKey(identifier)) {
                    effect = (PotionEffect) Config.EFFECTS.get(identifier);
                } else {
                    org.spongepowered.api.effect.potion.PotionEffect base = Serializers.POTION_TYPE.deserialize(node);
                    effect = new PotionEffect(identifier, base.type(), base.amplifier(), false, true);
                    Config.EFFECTS.put(effect.id, effect);
                }
            }

            int duration = Math.min(1, node.node("duration").getInt(11));
            return new EffectHolder<>(effect, duration);
        }
    }

}