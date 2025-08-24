package dev.flashlabs.cratecrate.internal;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.*;
import dev.flashlabs.cratecrate.component.key.Key;
import dev.flashlabs.flashlibs.inventory.Action;
import dev.flashlabs.flashlibs.inventory.Element;
import dev.flashlabs.flashlibs.inventory.Page;
import dev.flashlabs.flashlibs.inventory.View;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Utils {

    public static Page preview(Crate crate, Element back) {
        List<Element> list = new ArrayList<>();

        System.out.println("Test: " + crate.rewards().size() + " " + crate.rewards());

        for (RewardValueHolder r : crate.rewards()) {
            Element element = Element.of(r.icon(), new Consumer<Action.Click>() {
                @Override
                public void accept(Action.Click a) {
                    a.callback(v -> {
                        preview(r, Element.of(r.icon(), new Consumer<Action.Click>() {
                            @Override
                            public void accept(Action.Click a2) {
                                a2.callback(v2 -> {
                                    v2.open(a2.getPlayer());
                                });
                            }
                        })).open(a.getPlayer());
                    });
                }
            });
            list.add(element);
        }
        return Inventory.page(
                crate.name(Optional.empty()),
                list,
                back
        );
    }

    public static Page preview(RewardValueHolder reward, Element back) {
        return Inventory.page(
                reward.name(),
                reward.prizes().stream()
                        .map(ValueHolder::icon).map(Element::of)
                        .collect(Collectors.toList()),
                back
        );
    }

    public static View confirm(Tuple<Registration, ServerLocation> tuple) {
        var crate = tuple.first().crate();

        return Inventory.menu(crate.name(Optional.empty()), Map.of(
                10, Element.of(Inventory.item(ItemTypes.SLIME_BALL.get(), Component.text("Confirm")), a -> a.callback(v -> {
                    a.getPlayer().closeInventory();
                    if (checkKeys(a.getPlayer(), crate) && takeKeys(a.getPlayer(), crate)) {
                        crate.open(a.getPlayer(), tuple.second());
                    }
                })),
                13, Element.of(crate.icon(Optional.empty()), a -> a.callback(v -> {
                    preview(crate, Element.of(crate.icon(Optional.empty()), a2 -> a2.callback(v2 -> {
                        v.open(a2.getPlayer());
                    }))).open(a.getPlayer());
                })),
                16, Element.of(Inventory.item(ItemTypes.MAGMA_CREAM.get(), Component.text("Cancel")), a -> a.callback(v -> {
                    a.getPlayer().closeInventory();
                }))
        ));
    }

    public static boolean checkKeys(ServerPlayer player, Crate crate) {
        List<KeyHolder<? extends Key>> missing = crate.keys().stream()
                .filter(k -> !k.check(player.user()))
                .toList();
        if (!missing.isEmpty()) {
            CrateCrate.get().sendMessage(player, "interact.keys.missing",
                    "keys", Component.join(
                                    JoinConfiguration.separator(Component.text(", ")),
                                    missing.stream()
                                .map(ValueHolder::name)
                                .collect(Collectors.toList())));
        }

        return missing.isEmpty();
    }

    public static boolean takeKeys(ServerPlayer player, Crate crate) {
        List<KeyHolder<? extends Key>> taken = new ArrayList<>();
        for (KeyHolder<? extends Key> key : crate.keys()) {
            if (!key.take(player.user())) {
                if (taken.isEmpty()) {
                    CrateCrate.get().sendMessage(player, "interact.keys.take.failure",
                            "key", key.name()
                    );
                } else {
                    CrateCrate.get().getContainer().logger().error("Incomplete transaction for player " + player.name() + ": " + taken.stream()
                            .map(k -> k.id() + " (x" + k.value() + ")")
                            .collect(Collectors.joining(", ")));
                    CrateCrate.get().sendMessage(player, "interact.keys.take.incomplete",
                            "key", key.name(),
                            "keys", Component.join(
                                JoinConfiguration.separator(Component.text(", ")),
                                taken.stream()
                                        .map(ValueHolder::name)
                                        .toList()));
                }
                return false;
            }
            taken.add(key);
        }
        return true;
    }

    public static User user(CommandContext context, String name) throws CommandException {
        try {
            return Sponge.server().userManager().load(context.requireOne(Parameter.key("user", UUID.class))).get()
                    .orElseThrow(() -> new CommandException(Component.text("Invalid user.")));
        } catch (InterruptedException | ExecutionException e) {
            throw new CommandException(Component.text("Unable to load user."));
        }
    }
}
