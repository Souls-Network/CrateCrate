package dev.flashlabs.cratecrate.component.opener;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.Reward;
import dev.flashlabs.cratecrate.internal.Inventory;
import dev.flashlabs.cratecrate.internal.Utils;
import dev.flashlabs.flashlibs.inventory.Element;
import dev.flashlabs.flashlibs.inventory.Layout;
import dev.flashlabs.flashlibs.inventory.View;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.configurate.ConfigurationNode;

import java.math.BigDecimal;
import java.util.Optional;

public final class GuiOpener extends Opener {

    private static final GuiOpener INSTANCE = new GuiOpener();

    @Override
    public boolean open(ServerPlayer player, Crate crate, ServerLocation location) {
        Tuple<? extends Reward, BigDecimal> reward = crate.roll(player);
        View.builder(ContainerTypes.GENERIC_3X3.get())
            .title(crate.name(Optional.empty()))
            .build(CrateCrate.get().getContainer())
            .define(Layout.builder(3, 3)
                .set(Element.of(reward.first().icon(Optional.of(reward.second())), a -> a.callback(v -> {
                    Utils.preview(reward, Element.of(crate.icon(Optional.empty()), a2 -> a2.callback(v2 -> {
                        v.open(a2.getPlayer());
                    }))).open(a.getPlayer());
                })), 4)
                .set(Element.of(Inventory.item(ItemTypes.YELLOW_STAINED_GLASS_PANE.get())), 0, 2, 6, 8)
                .set(Element.of(Inventory.item(ItemTypes.ORANGE_STAINED_GLASS_PANE.get())), 1, 3, 5, 7)
                .build())
            .open(player);

        return crate.give(player, reward, location);
    }

    public static GuiOpener deserialize(ConfigurationNode node) {
        return INSTANCE;
    }

}