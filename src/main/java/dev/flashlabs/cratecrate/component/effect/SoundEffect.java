package dev.flashlabs.cratecrate.component.effect;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.math.vector.Vector3d;

import java.util.List;
import java.util.Optional;

public final class SoundEffect extends Effect.Locatable {

    public static final Type<SoundEffect, Tuple<Target, Vector3d>> TYPE = new SoundEffectType();

    private final SoundType type;
    private final float volume;
    private final float pitch;

    private SoundEffect(
        String id,
        SoundType type,
        float volume,
        float pitch
    ) {
        super(id);
        this.type = type;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public Component name(Optional<Tuple<Target, Vector3d>> value) {
        return Component.text(type.key().asString());
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
        location.world().playSound(
            Sound.sound().type(type)
                    .volume(volume).pitch(pitch).build(),
                location.position()
        );
        return true;
    }

    private static final class SoundEffectType extends Type<SoundEffect, Tuple<Target, Vector3d>> {

        private SoundEffectType() {
            super("Sound", CrateCrate.get().getContainer());
        }

        @Override
        public SoundEffect deserializeComponent(ConfigurationNode node) throws SerializationException {
            SoundType type = Serializers.SOUND_TYPE.deserialize(node.node("sound"));
            float volume = node.node("sound.volume").getFloat(1.0f);
            float pitch = node.node("sound.pitch").getFloat(1.0f);
            return new SoundEffect(String.valueOf(node.key()), type, volume, pitch);
        }

        @Override
        public Tuple<SoundEffect, Tuple<Target, Vector3d>> deserializeReference(ConfigurationNode node, List<? extends ConfigurationNode> values) throws SerializationException {
            SoundEffect effect;
            if (node.isMap()) {
                effect = deserializeComponent(node);
                effect = new SoundEffect("SoundEffect@" + node.path(), effect.type, effect.volume, effect.pitch);
                Config.EFFECTS.put(effect.id, effect);
            } else {
                String identifier = node.getString();
                if (Config.EFFECTS.containsKey(identifier)) {
                    effect = (SoundEffect) Config.EFFECTS.get(identifier);
                } else {
                    SoundType type = node.get(Serializers.CATALOG_TYPE.of(SoundType.class));
                    effect = new SoundEffect(identifier, type, Optional.empty(), Optional.empty());
                    Config.EFFECTS.put(effect.id, effect);
                }
            }
            return Tuple.of(effect, deserializeReferenceValue(node, values));
        }

    }

}