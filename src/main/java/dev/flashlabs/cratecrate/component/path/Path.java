package dev.flashlabs.cratecrate.component.path;

import dev.flashlabs.cratecrate.internal.Serializers;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

public abstract class Path {

    //TODO: Allow registration of custom path types
    public enum Type {
        CIRCLE,
        HELIX,
        SPIRAL,
        VORTEX;

        public static Type fromString(@Nullable String type) {
            return type != null ? Type.valueOf(type.toUpperCase()) : null;
        }
    }

    private final int interval;
    private final int precision;
    private final int segments;
    private final double shift;
    private final double speed;
    private final Vector3d scale;

    Path(
        int interval,
        int precision,
        int segments,
        double shift,
        double speed,
        Vector3d scale
    ) {
        this.interval = interval;
        this.precision = precision;
        this.segments = segments;
        this.shift = shift;
        this.speed = speed;
        this.scale = scale;
    }

    public final int interval() {
        return interval;
    }

    public final int precision() {
        return precision;
    }

    public final int segments() {
        return segments;
    }

    public final double shift() {
        return shift;
    }

    public final double speed() {
        return speed;
    }

    public final Vector3d scale() {
        return scale;
    }

    public abstract Vector3d[] positions(double radians);

    public static Path deserialize(ConfigurationNode node) {
        var type = Optional.ofNullable(node.isMap() ? node.getString() : node.node("type").getString()).map(String::toUpperCase).map(Type::valueOf).orElseThrow(AssertionError::new);

        return switch (type) {
            case CIRCLE -> CirclePath.deserialize(node);
            case HELIX -> HelixPath.deserialize(node);
            case SPIRAL -> SpiralPath.deserialize(node);
            case VORTEX -> VortexPath.deserialize(node);
        };
    }

}