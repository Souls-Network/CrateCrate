package dev.flashlabs.cratecrate.task;

import dev.flashlabs.cratecrate.CrateCrate;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;

import java.util.function.Consumer;

public class TaskUtils {

    public static ScheduledTask runTask(Consumer<Task.Builder> consumer) {
        var builder = Task.builder().plugin(CrateCrate.get().getContainer());
        consumer.accept(builder);

        return Sponge.server().scheduler().submit(builder.build());
    }

    public static ScheduledTask execute(Consumer<ScheduledTask> consumer) {
        return runTask(builder -> builder.execute(consumer));
    }

    public static ScheduledTask execute(Runnable consumer) {
        return runTask(builder -> builder.execute(consumer));
    }
}
