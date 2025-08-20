package dev.flashlabs.cratecrate.component.opener;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.Reward;
import dev.flashlabs.cratecrate.component.RewardValueHolder;
import dev.flashlabs.cratecrate.internal.Inventory;
import dev.flashlabs.cratecrate.internal.Utils;
import dev.flashlabs.flashlibs.inventory.Element;
import dev.flashlabs.flashlibs.inventory.Layout;
import dev.flashlabs.flashlibs.inventory.View;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RouletteOpener extends Opener {

    private static final RouletteOpener INSTANCE = new RouletteOpener();
    private static final List<Element> PANES = List.of(
        Element.of(Inventory.item(ItemTypes.RED_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.ORANGE_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.YELLOW_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.LIME_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.LIGHT_BLUE_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.BLUE_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.PURPLE_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.MAGENTA_STAINED_GLASS_PANE.get())),
        Element.of(Inventory.item(ItemTypes.PINK_STAINED_GLASS_PANE.get()))
    );
    private static final Set<Integer> TIMES = Set.of(
        14, 16, 18, 20, 22, 25, 28, 31, 35,
        39, 44, 49, 55, 62, 70, 79, 89, 100
    );
    private static final Random RANDOM = new Random();

    @Override
    public boolean open(ServerPlayer player, Crate crate, ServerLocation location) {

        AtomicBoolean received = new AtomicBoolean(false);
        View view = View.builder(ContainerTypes.GENERIC_9X3.get())
            .title(crate.name(Optional.empty()))
            .onClose(a -> a.setCancelled(!received.get()))
            .build(CrateCrate.get().getContainer());
        var task = Task.builder().plugin(CrateCrate.get().getContainer())
            .execute(new Consumer<ScheduledTask>() {

                private int frame = 0;
                private final int selection = RANDOM.nextInt(9);
                private final List<Element> panes = new ArrayList<>(PANES);
                private final List<RewardValueHolder> rewards = IntStream.range(0, 9)
                    .mapToObj(i -> crate.roll(player))
                    .toList();
                private final List<Element> icons = rewards.stream()
                    .map(r -> Element.of(r.icon(), a -> a.callback(v -> {
                        if (received.get()) {
                            Utils.preview(r, Element.of(crate.icon(Optional.empty()), a2 -> a2.callback(v2 -> {
                                v.open(a2.getPlayer());
                            }))).open(a.getPlayer());
                        }
                    })))
                    .collect(Collectors.toList());

                @Override
                public void accept(ScheduledTask task) {
                    Layout.Builder builder = Layout.builder(3, 9);
                    for (int i = 0; i < 9; i++) {
                        Element pane = i != 4 ? panes.get(i) : Element.of(ItemStack.builder()
                            .fromSnapshot(panes.get(i).getItem())
                            .add(Keys.APPLIED_ENCHANTMENTS, List.of(Enchantment.of(EnchantmentTypes.POWER.get(), 1)))
                            .build());
                        builder.set(pane, i, i + 18);
                    }
                    Collections.rotate(panes, -1);
                    if (frame++ + selection <= 13 || TIMES.contains(frame + selection)) {
                        Collections.rotate(icons, 1);
                        for (int i = 0; i < 9; i++) {
                            builder.set(icons.get(i), i + 9);
                        }
                        if (frame + selection == 100) {
                            crate.give(player, rewards.get(selection), location);
                            received.getAndSet(true);
                            task.cancel();
                        }
                    }
                    view.update(builder.build());
                }

            }).interval(Ticks.of(1)).build();

        Sponge.server().scheduler().submit(task);
        view.open(player);
        return true;
    }

    public static RouletteOpener deserialize(ConfigurationNode node) {
        return INSTANCE;
    }
}