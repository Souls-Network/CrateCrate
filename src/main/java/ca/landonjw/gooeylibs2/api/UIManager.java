package ca.landonjw.gooeylibs2.api;

import ca.landonjw.gooeylibs2.api.container.GooeyContainer;
import ca.landonjw.gooeylibs2.api.page.Page;
import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.task.TaskUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class UIManager {

    public static void openUIPassively(@NonNull ServerPlayer player, @NonNull Page page, long timeout, TimeUnit timeoutUnit) {
        AtomicLong timeOutTicks = new AtomicLong(timeoutUnit.convert(timeout, TimeUnit.SECONDS) * 20);
        Sponge.server().scheduler().submit(Task.builder()
                .execute((task) -> {
                    timeOutTicks.getAndDecrement();

                    if (timeOutTicks.get() <= 0) {
                        openUIForcefully(player, page);
                        task.cancel();
                    }
                })
                .interval(Ticks.of(1))
                .build());
    }

    public static void openUIForcefully(@NonNull ServerPlayer player, @NonNull Page page) {
        // Delay the open to allow sponge's annoying mixins to process previous container and not have aneurysm
        TaskUtils.execute(() -> {
                    try {
                        GooeyContainer container = new GooeyContainer(player, page);
                        container.open();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
    }

    public static void closeUI(@NonNull ServerPlayer player) {
        TaskUtils.execute(player::closeInventory);

    }

}