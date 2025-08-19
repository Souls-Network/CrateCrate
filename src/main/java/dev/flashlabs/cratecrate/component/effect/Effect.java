package dev.flashlabs.cratecrate.component.effect;

import dev.flashlabs.cratecrate.component.Component;
import dev.flashlabs.cratecrate.component.Type;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.math.vector.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Effect<T> extends Component<T> {

    public static final Map<String, Type<? extends Effect, ?>> TYPES = new HashMap<>();

    public enum Action {
        IDLE,
        OPEN,
        GIVE,
        REJECT,
        PREVIEW,
    }

    protected Effect(String id) {
        super(id);
    }

    public abstract boolean give(Player player, ServerLocation location, T value);

    public static abstract class Locatable extends Effect<Tuple<Locatable.Target, Vector3d>> {

        public enum Target {
            PLAYER,
            LOCATION
        }

        protected Locatable(String id) {
            super(id);
        }

        @Override
        public boolean give(Player player, ServerLocation location, Tuple<Target, Vector3d> value) {
            return give((value.first() == Target.PLAYER ? player.serverLocation() : location).add(value.second()));
        }

        public abstract boolean give(ServerLocation location);

        protected static Tuple<Target, Vector3d> deserializeReferenceValue(ConfigurationNode node, List<? extends ConfigurationNode> values) throws SerializationException {
            Target target = Target.LOCATION;
            Vector3d offset = Vector3d.ZERO;

            if (!node.empty() && node.isMap()) {
                // object form: { target: ..., offset: [x, y, z] }
                target = node.node("target").get(Target.class, Target.LOCATION);

                ConfigurationNode offsetNode = node.node("offset");
                double x = offsetNode.node(0).getDouble(0.0);
                double y = offsetNode.node(1).getDouble(0.0);
                double z = offsetNode.node(2).getDouble(0.0);
                offset = Vector3d.from(x, y, z);

            } else if (values.size() == 1) {
                // single value = target enum
                target = values.get(0).get(Target.class);

            } else if (values.size() == 3) {
                // three doubles = offset
                offset = Vector3d.from(
                        values.get(0).getDouble(),
                        values.get(1).getDouble(),
                        values.get(2).getDouble()
                );

            } else if (values.size() == 4) {
                // enum + three doubles
                target = values.get(0).get(Target.class);
                offset = Vector3d.from(
                        values.get(1).getDouble(),
                        values.get(2).getDouble(),
                        values.get(3).getDouble()
                );
            }

            return Tuple.of(target, offset);
        }

    }

}