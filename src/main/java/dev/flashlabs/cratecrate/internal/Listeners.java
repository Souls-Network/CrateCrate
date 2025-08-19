package dev.flashlabs.cratecrate.internal;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.key.Key;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Listeners {

    @Listener
    public void onInteractBlockPrimary(InteractBlockEvent.Primary.Start event, @Root ServerPlayer player) {
        event.block().location().flatMap(l -> preInteract(event, player, l)).ifPresent(t -> {
            if (!player.hasPermission("cratecrate.crates." + t.first().id() + ".preview")) {
                CrateCrate.get().sendMessage(player, "interact.crates.preview.no-permission",
                        "create", t.first().name(Optional.empty()));
            } else {
                Utils.preview(t.first(), Inventory.CLOSE).open(player);
            }
        });
    }

    @Listener
    public void onInteractBlockSecondary(InteractBlockEvent.Secondary event, @Root ServerPlayer player) {
        event.block().location().flatMap(l -> preInteract(event, player, l)).ifPresent(t -> {
           if(Utils.checkKeys(player, t.first())) {
               Utils.confirm(t).open(player);
           }
        });
    }

    private <T extends InteractEvent & Cancellable> Optional<Tuple<Crate, ServerLocation>> preInteract(T event, ServerPlayer player, ServerLocation location) {
        return Optional.ofNullable(Storage.LOCATIONS.get(location)).flatMap(o -> {
            event.setCancelled(true);
            if (o.isEmpty()) {
                CrateCrate.get().sendMessage(player, "interact.crates.unavailable");
            } else if (!player.hasPermission("cratecrate.crates." + o.get().id() + ".base")) {
                CrateCrate.get().sendMessage(player, "interact.crates.no-permission",
                        "crate", o.get().name(Optional.empty())
                );
                o.get().effects().get(Effect.Action.REJECT).forEach(e -> e.getFirst().give(player, location, e.getSecond()));
            } else {
                return o
                        .filter(c -> event.context().get(EventContextKeys.USED_HAND).map(a -> a.equals(HandTypes.MAIN_HAND.get())).orElse(false))
                        .map(c -> Tuple.of(c, location.add(0.5, 0.5, 0.5)));
            }
            return Optional.empty();
        });
    }

}
