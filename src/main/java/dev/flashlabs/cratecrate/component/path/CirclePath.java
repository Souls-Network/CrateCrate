package dev.flashlabs.cratecrate.component.path;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.math.TrigMath;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.vector.Vector3d;

public final class CirclePath extends Path {

    private final Vector3d axis;

    public CirclePath(
        int interval,
        int precision,
        int segments,
        double shift,
        double speed,
        Vector3d scale,
        Vector3d axis
    ) {
        super(interval, precision, segments, shift, speed, scale);
        this.axis = axis;
    }

    @Override
    public Vector3d[] positions(double radians) {
        Vector3d[] vectors = new Vector3d[segments()];
        double[] shifts = AnimationUtils.shift(radians, segments());
        for (int i = 0; i < shifts.length; i++) {
            vectors[i] = axis.equals(Vector3d.UNIT_Y)
                ? Vector3d.from(TrigMath.cos(shifts[i]), 0.0, TrigMath.sin(shifts[i]))
                : Quaterniond.fromAngleRadAxis(shifts[i], axis).rotate(Vector3d.from(-axis.z(), 0.0, axis.x()).normalize());
        }
        return vectors;
    }

    public static CirclePath deserialize(ConfigurationNode node) {
        int interval = node.node("interval").getInt(20);
        int precision = node.node("precision").getInt(120);
        int segments = node.node("segments").getInt(1);
        double shift = node.node("shift").getDouble(0.0);
        double speed = node.node("speed").getDouble(1.0);

        Vector3d scale = Vector3d.ONE;

        if(node.hasChild("scale")) {
            scale = Vector3d.from(
                    node.node("scale").childrenList().get(0).getDouble(1.0),
                    node.node("scale").childrenList().get(1).getDouble(1.0),
                    node.node("scale").childrenList().get(2).getDouble(1.0)
            );
        }

        Vector3d axis = Vector3d.UNIT_Y;

        if(node.hasChild("axis")) {
            axis = Vector3d.from(
                    node.node("axis").childrenList().get(0).getDouble(0.0),
                    node.node("axis").childrenList().get(1).getDouble(1.0),
                    node.node("axis").childrenList().get(2).getDouble(0.0)
            );
        }


        System.out.println("Loading: circle");

        return new CirclePath(interval, precision, segments, shift, speed, scale, axis);
    }

}