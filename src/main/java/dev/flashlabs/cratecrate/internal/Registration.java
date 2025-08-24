package dev.flashlabs.cratecrate.internal;

import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.effect.Effect;
import dev.flashlabs.cratecrate.component.effect.EffectHolder;
import dev.flashlabs.cratecrate.component.effect.ParticleEffect;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

public final class Registration {

    private final ServerLocation location;
    private final Crate crate;
    private final List<ScheduledTask> effects = new ArrayList<>();

    public Registration(ServerLocation location, Crate crate) {
        this.location = location;
        this.crate = crate;
    }

    public ServerLocation location() {
        return location;
    }

    public Crate crate() {
        return crate;
    }

    public void startEffects() {
        stopEffects();
        //TODO
        for (EffectHolder<?, ?> e : crate.effects().getOrDefault(Effect.Action.IDLE, List.of())) {
            if (e.component() instanceof ParticleEffect particleEffect) {
                System.out.println("Say GERMINO!");

                effects.add(particleEffect.start(location.add(0.5, 0.5, 0.5).add(((Tuple<Effect.Locatable.Target, Vector3d>) e.value()).second())));
            }
        }
    }

    public void stopEffects() {
        effects.forEach(ScheduledTask::cancel);
        effects.clear();
    }

}