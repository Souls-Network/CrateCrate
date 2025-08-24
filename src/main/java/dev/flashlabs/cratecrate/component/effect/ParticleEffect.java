package dev.flashlabs.cratecrate.component.effect;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.component.ValueHolder;
import dev.flashlabs.cratecrate.component.path.Path;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.math.TrigMath;
import org.spongepowered.math.vector.Vector3d;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ParticleEffect extends Effect.Locatable {

    public static final Type<ParticleEffect> TYPE = new ParticleEffectType();

    private final ParticleType type;
    private final Optional<Color> color;
    private final Path path;

    private ParticleEffect(
        String id,
        ParticleType type,
        Optional<Color> color,
        Path path
    ) {
        super(id);
        this.type = type;
        this.color = color;
        this.path = path;
    }

    @Override
    public Component name(Optional<Tuple<Target, Vector3d>> value) {
        return LinearComponents.linear(Component.text(type.key(RegistryTypes.PARTICLE_TYPE).asString()), type.equals(ParticleTypes.DUST)
            ? LinearComponents.linear(Component.text(" ("), Component.text(color.map(c -> Integer.toString(c.rgb(), 16).toUpperCase()).orElse("Random")), Component.text(")"))
            : Component.empty());
    }

    /**
     * Returns the lore of this effect, which is always empty.
     */
    @Override
    public List<Component> lore(Optional<Tuple<Target, Vector3d>> value) {
        return List.of();
    }

    /**
     * Returns the icon of this effect, which is a redstone item with this
     * effect's name/lore.
     */
    @Override
    public ItemStack icon(Optional<Tuple<Target, Vector3d>> value) {
        return ItemStack.builder()
            .itemType(ItemTypes.REDSTONE)
            .add(Keys.DISPLAY_NAME, name(value))
            .add(Keys.LORE, lore(value))
            .build();
    }

    @Override
    public boolean give(ServerLocation location) {
        ScheduledTask task = start(location);
        var canceltask = Task.builder()
            .execute(task::cancel)
            .delay((long) path.interval() * path.precision(), TimeUnit.MILLISECONDS)
            .plugin(CrateCrate.get().getContainer()).build();
        Sponge.server().scheduler().submit(canceltask);

        return true;
    }

    public ScheduledTask start(ServerLocation location) {
        var task = Task.builder()
            .execute(new Runnable() {

                private double radians = path.shift();
                private final double increment = path.speed() * (TrigMath.TWO_PI / path.precision());
                private final org.spongepowered.api.effect.particle.ParticleEffect.Builder builder = org.spongepowered.api.effect.particle.ParticleEffect.builder().type(type);

                @Override
                public void run() {
                    if (type.equals(ParticleTypes.DUST.get())) {
                        builder.option(ParticleOptions.COLOR, color.orElseGet(() -> Color.ofRgb(
                            (int) (127.5 + 127.5 * TrigMath.cos(6.0 / 7.0 * radians)),
                            (int) (127.5 + 127.5 * TrigMath.sin(6.0 / 7.0 * radians)),
                            (int) (127.5 - 127.5 * TrigMath.cos(6.0 / 7.0 * radians))
                        )));
                    }
                    org.spongepowered.api.effect.particle.ParticleEffect effect = builder.build();

                    for (Vector3d vector : path.positions(radians)) {
                        location.world().spawnParticles(effect, location.position().add(vector.mul(path.scale())));
                    }
                    radians += increment;
                }

            })
            .interval(path.interval(), TimeUnit.MILLISECONDS)
                .plugin(CrateCrate.get().getContainer()).build();

        return Sponge.server().scheduler().submit(task);

    }

    private static final class ParticleEffectType extends Type<ParticleEffect> {

        private ParticleEffectType() {
            super("Particle", CrateCrate.get().getContainer());
        }

        /**
         * Deserializes a sound effect, defined as:
         *
         * <pre>{@code
         * SoundEffect:
         *     sound: String (SoundType) | Object
         *         type: String (SoundType)
         *         volume: Optional<Double>
         *         pitch: Optional<Double>
         * }</pre>
         */
        @Override
        public ParticleEffect deserializeComponent(String id, ConfigurationNode node) throws SerializationException {
            ParticleType type = Serializers.PARTICLE_TYPE.deserialize(node.node("particle"));
            Optional<Color> color = Optional.ofNullable(node.node("particle.color")).map(ConfigurationNode::getInt).map(Color::ofRgb);
            Path path = Path.deserialize(node.node("path"));
            return new ParticleEffect(id, type, color, path);
        }

        /**
         * Deserializes a particle effect reference, defined as:
         *
         * <pre>{@code
         * SoundEffectReference:
         *     node:
         *        ParticleEffect |
         *        String (ParticleEffect id)
         *     values: Effect.Locatable reference value
         * }</pre>
         */
        @Override
        public ValueHolder<ParticleEffect, ?> deserializeReference(ConfigurationNode node) throws SerializationException {
            ParticleEffect effect;
            if (node.isMap()) {
                effect = deserializeComponent("ParticleEffect@" + node.path(), node);
                Config.EFFECTS.put(effect.id, effect);
            } else {
                String identifier = node.getString();
                if (Config.EFFECTS.containsKey(identifier)) {
                    effect = (ParticleEffect) Config.EFFECTS.get(identifier);
                } else {
                    throw new AssertionError();
                }
            }
            return new EffectHolder<>(effect, deserializeReferenceValue(node));
        }

    }

}