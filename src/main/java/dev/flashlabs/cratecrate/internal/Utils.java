package dev.flashlabs.cratecrate.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.Reward;
import dev.flashlabs.cratecrate.component.key.Key;
import dev.flashlabs.flashlibs.inventory.Element;
import dev.flashlabs.flashlibs.inventory.Page;
import dev.flashlabs.flashlibs.inventory.View;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Utils {

    public static Page preview(Crate crate, Element back) {
        return Inventory.page(
                crate.name(Optional.empty()),
                crate.rewards().stream()
                        .map(r -> Element.of(r.first().icon(Optional.of(r.second())), a -> a.callback(v -> {
                            preview(r, Element.of(r.first().icon(Optional.empty()), a2 -> a2.callback(v2 -> {
                                v.open(a2.getPlayer());
                            }))).open(a.getPlayer());
                        })))
                        .collect(Collectors.toList()),
                back
        );
    }

    public static Page preview(Tuple<? extends Reward, BigDecimal> reward, Element back) {
        return Inventory.page(
                reward.first().name(Optional.of(reward.second())),
                reward.first().prizes().stream()
                        .map(p -> Element.of(p.first().icon(Optional.of(p.second()))))
                        .collect(Collectors.toList()),
                back
        );
    }

    public static View confirm(Tuple<Crate, ServerLocation crate) {
        return Inventory.menu(crate.first().name(Optional.empty()), ImmutableMap.of(
                10, Element.of(Inventory.item(ItemTypes.SLIME_BALL.get(), Component.text("Confirm")), a -> a.callback(v -> {
                    a.getPlayer().closeInventory();
                    if (checkKeys(a.getPlayer(), crate.first()) && takeKeys(a.getPlayer(), crate.first())) {
                        crate.first().open(a.getPlayer(), crate.second());
                    }
                })),
                13, Element.of(crate.first().icon(Optional.empty()), a -> a.callback(v -> {
                    preview(crate.first(), Element.of(crate.first().icon(Optional.empty()), a2 -> a2.callback(v2 -> {
                        v.open(a2.getPlayer());
                    }))).open(a.getPlayer());
                })),
                16, Element.of(Inventory.item(ItemTypes.MAGMA_CREAM.get(), Component.text("Cancel")), a -> a.callback(v -> {
                    a.getPlayer().closeInventory();
                }))
        ));
    }

    public static boolean checkKeys(ServerPlayer player, Crate crate) {
        List<Tuple<? extends Key, Integer>> missing = crate.keys().stream()
                .filter(k -> !k.first().check(player.user(), k.second()))
                .toList();
        if (!missing.isEmpty()) {
            CrateCrate.get().sendMessage(player, "interact.keys.missing",
                    "keys", LinearComponents.linear(component -> {
                        component.append(Component.text(", "));
                        component.append(missing.stream()
                                .map(k -> k.first().name(Optional.of(k.second())))
                                .collect(Collectors.toList()));
                    }));
        }

        return missing.isEmpty();
    }

    public static boolean takeKeys(ServerPlayer player, Crate crate) {
        List<Tuple<? extends Key, Integer>> taken = Lists.newArrayList();
        for (Tuple<? extends Key, Integer> key : crate.keys()) {
            if (!key.first().take(player.user(), key.second())) {
                if (taken.isEmpty()) {
                    CrateCrate.get().sendMessage(player, "interact.keys.take.failure");
                } else {
                    CrateCrate.get().getContainer().logger().error("Incomplete transaction for player " + player.name() + ": " + taken.stream()
                            .map(k -> k.first().id() + " (x" + k.second() + ")")
                            .collect(Collectors.joining(", ")));
                    CrateCrate.get().sendMessage(player, "interact.keys.take.incomplete",
                            "keys", LinearComponents.linear(component -> {
                                component.append(Component.text(", "));
                                component.append(taken.stream()
                                        .map(k -> k.first().name(Optional.of(k.second())))
                                        .toList());
                            }));
                }
                return false;
            }
            taken.add(key);
        }
        return true;
    }
}
