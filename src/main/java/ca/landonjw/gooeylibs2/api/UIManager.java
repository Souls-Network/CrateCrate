package ca.landonjw.gooeylibs2.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class UIManager {

    public static void openUIPassively(@NonNull ServerPlayer player, @NonNull Page page, long timeout, TimeUnit timeoutUnit) {
        AtomicLong timeOutTicks = new AtomicLong(timeoutUnit.convert(timeout, TimeUnit.SECONDS) * 20);
        Task.builder()
                .execute((task) -> {
                    timeOutTicks.getAndDecrement();

                    if (player.containerMenu.containerId == ((ServerPlayerAccessor) player).gooeylibs$getContainerCounter() || timeOutTicks.get() <= 0) {
                        openUIForcefully(player, page);
                        task.setExpired();
                    }
                })
                .infinite()
                .interval(1)
                .build();
    }

    public static void openUIForcefully(@NotNull ServerPlayer player, @NotNull Page page) {
        // Delay the open to allow sponge's annoying mixins to process previous container and not have aneurysm
        Task.builder()
                .execute(() -> {
                    try {
                        GooeyContainer container = new GooeyContainer(player, page);
                        container.open();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                })
                .build();
    }

    public static void closeUI(@NotNull ServerPlayer player) {
        Task.builder().execute(player::closeContainer).build();
    }

}