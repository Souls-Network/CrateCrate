package dev.flashlabs.cratecrate.component.effect;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.FireworkShape;
import org.spongepowered.api.item.FireworkShapes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.math.vector.Vector3d;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public final class FireworkEffect extends Effect.Locatable {

    public static final Type<FireworkEffect, Tuple<Target, Vector3d>> TYPE = new FireworkEffectType();

    private static final Random RANDOM = new Random();

    private final Optional<FireworkShape> shape;
    private final Optional<List<Color>> colors;
    private final Optional<List<Color>> fades;
    private final Optional<Boolean> trail;
    private final Optional<Boolean> flicker;
    private final Optional<Integer> duration;

    private FireworkEffect(
        String id,
        Optional<FireworkShape> shape,
        Optional<List<Color>> colors,
        Optional<List<Color>> fades,
        Optional<Boolean> trail,
        Optional<Boolean> flicker,
        Optional<Integer> duration
    ) {
        super(id);
        this.shape = shape;
        this.colors = colors;
        this.fades = fades;
        this.trail = trail;
        this.flicker = flicker;
        this.duration = duration;
    }

    @Override
    public Component name(Optional<Tuple<Target, Vector3d>> value) {
        return Component.text("Firework (" + shape.map(a -> a.key(RegistryTypes.FIREWORK_SHAPE).asString()).orElse("Random") + ")");
    }

    @Override
    public List<Component> lore(Optional<Tuple<Target, Vector3d>> value) {
        return List.of();
    }

    @Override
    public ItemStack icon(Optional<Tuple<Target, Vector3d>> value) {
        return ItemStack.builder()
            .itemType(ItemTypes.MUSIC_DISC_13)
            .add(Keys.DISPLAY_NAME, name(value))
            .add(Keys.LORE, lore(value))
            .build();
    }

    @Override
    public boolean give(ServerLocation location) {
        Entity firework = location.world().createEntity(EntityTypes.FIREWORK_ROCKET, location.position());
        firework.offer(Keys.FIREWORK_EFFECTS, List.of(org.spongepowered.api.item.FireworkEffect.builder()
            .shape(shape.orElseGet(() -> {
                Collection<FireworkShape> shapes = Sponge.game().registry(RegistryTypes.FIREWORK_SHAPE).stream().toList();
                return shapes.stream().skip(RANDOM.nextInt(shapes.size())).findFirst().orElse(FireworkShapes.SMALL_BALL.get());
            }))
            .colors(colors.orElseGet(() -> RANDOM.nextBoolean()
                ? List.of(Color.ofRgb(RANDOM.nextInt(0xFFFFFF)))
                : List.of(Color.ofRgb(RANDOM.nextInt(0xFFFFFF)), Color.ofRgb(RANDOM.nextInt(0xFFFFFF)))
            ))
            .fades(fades.orElseGet(() -> RANDOM.nextBoolean()
                ? List.of()
                : List.of(Color.ofRgb(RANDOM.nextInt(0xFFFFFF)))
            ))
            .trail(trail.orElseGet(RANDOM::nextBoolean))
            .flicker(flicker.orElseGet(RANDOM::nextBoolean))
            .build()));
        firework.offer(Keys.FIREWORK_FLIGHT_MODIFIER, Ticks.of(duration.orElseGet(() -> 1 + RANDOM.nextInt(3))));
        firework.offer(Keys.EXPLOSION_RADIUS, 0);
        return location.world().spawnEntity(firework);
    }

    private static final class FireworkEffectType extends Type<FireworkEffect, Tuple<Target, Vector3d>> {

        private FireworkEffectType() {
            super("Firework", CrateCrate.get().getContainer());
        }

        @Override
        public FireworkEffect deserializeComponent(ConfigurationNode node) throws SerializationException {
            Optional<FireworkShape> shape = Serializers.FIREWORK_SHAPE.deserializeOptional(node.node("firework"));
            Optional<List<Color>> colors = Optional.ofNullable(node.node("firework.colors").getList(Integer.class)).map(l -> l.stream().map(Color::ofRgb).collect(Collectors.toList()));
            Optional<List<Color>> fades = Optional.ofNullable(node.node("firework.fades").getList(Integer.class)).map(l -> l.stream().map(Color::ofRgb).collect(Collectors.toList()));
            Optional<Boolean> trail = node.hasChild("firework.trail") ? Optional.of(node.node("firework.trail").getBoolean()) : Optional.empty();
            Optional<Boolean> flicker = node.hasChild("firework.flicker") ? Optional.of(node.node("firework.flicker").getBoolean()) : Optional.empty();
            Optional<Integer> duration = node.hasChild("firework.flicker") ? Optional.of(node.node("firework.duration").getInt()) : Optional.empty();
            return new FireworkEffect(String.valueOf(node.key()), shape, colors, fades, trail, flicker, duration);
        }

        @Override
        public Tuple<FireworkEffect, Tuple<Target, Vector3d>> deserializeReference(ConfigurationNode node, List<? extends ConfigurationNode> values) throws SerializationException {
            FireworkEffect effect;
            if (node.isMap()) {
                effect = deserializeComponent(node);
                effect = new FireworkEffect("FireworkEffect@" + node.path(), effect.shape, effect.colors, effect.fades, effect.trail, effect.flicker, effect.duration);
                Config.EFFECTS.put(effect.id, effect);
            } else {
                String identifier = node.getString();
                if (Config.EFFECTS.containsKey(identifier)) {
                    effect = (FireworkEffect) Config.EFFECTS.get(identifier);
                } else {
                    String[] split = identifier.split("/");
                    Optional<FireworkShape> shape = split.length == 2 ? Sponge.server().registry(RegistryTypes.FIREWORK_SHAPE).findValue(ResourceKey.resolve(split[1])) : Optional.empty();
                    effect = new FireworkEffect(identifier, shape, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                    Config.EFFECTS.put(effect.id, effect);
                }
            }
            return Tuple.of(effect, deserializeReferenceValue(node, values));
        }

    }

}