package dev.flashlabs.cratecrate.component.path;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.math.TrigMath;
import org.spongepowered.math.vector.Vector3d;

public final class HelixPath extends Path {

    public HelixPath(
        int interval,
        int precision,
        int segments,
        double shift,
        double speed,
        Vector3d scale
    ) {
        super(interval, precision, segments, shift, speed, scale);
    }

    @Override
    public Vector3d[] positions(double radians) {
        double y = TrigMath.sin(radians);
        Vector3d[] vectors = new Vector3d[segments()];
        double[] shifts = AnimationUtils.shift(radians, segments());
        for (int i = 0; i < shifts.length; i++) {
            vectors[i] = Vector3d.from(TrigMath.cos(shifts[i]), y, TrigMath.sin(shifts[i]));
        }
        return vectors;
    }

    public static HelixPath deserialize(ConfigurationNode node) {
        int interval = node.node("interval").getInt(20);
        int precision = node.node("precision").getInt(120);
        int segments = node.node("segments").getInt(1);
        double shift = node.node("shift").getDouble(0.0);
        double speed = node.node("speed").getDouble(1.0);
        Vector3d scale = Vector3d.from(
            node.node("scale").childrenList().get(0).getDouble (1.0),
            node.node("scale").childrenList().get(1).getDouble (1.0),
            node.node("scale").childrenList().get(2).getDouble (1.0)
        );

        return new HelixPath(interval, precision, segments, shift, speed, scale);
    }

}