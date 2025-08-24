package dev.flashlabs.cratecrate.internal;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.flashlibs.inventory.Element;
import dev.flashlabs.flashlibs.inventory.Layout;
import dev.flashlabs.flashlibs.inventory.Page;
import dev.flashlabs.flashlibs.inventory.View;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickType;

import java.util.List;
import java.util.Map;

public final class Inventory {

    public static final Element
            BACK = Element.of(ItemStack.empty()),
            CLOSE = Element.of(item(ItemTypes.BARRIER.get(), Component.text("Close")), a -> a.callback(v -> a.getPlayer().closeInventory()));
    public static final Layout
            MENU_LAYOUT = Layout.builder(3, 9)
            .set(Element.of(item(ItemTypes.STONE.get())), 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26)
//            .set(Element.of(item(ItemTypes.ORANGE_STAINED_GLASS_PANE.get())), 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25)
            .build(),
            PAGE_LAYOUT = Layout.builder(6, 9)
                    .set(Element.of(item(ItemTypes.YELLOW_STAINED_GLASS_PANE.get())), 0, 2, 4, 6, 8, 18, 26, 36, 38, 40, 42, 44, 46, 52)
                    .set(Element.of(item(ItemTypes.ORANGE_STAINED_GLASS_PANE.get())), 1, 3, 5, 7, 9, 17, 27, 35, 37, 39, 41, 43)
                    .set(BACK, 45)
                    .set(Page.FIRST, 47)
                    .set(Page.PREVIOUS, 48)
                    .set(Page.CURRENT, 49)
                    .set(Page.NEXT, 50)
                    .set(Page.LAST, 51)
                    .set(CLOSE, 53)
                    .build();

    public static View menu(Component title, Map<Integer, Element> elements) {
        System.out.println("LARP LARP " + elements);


        return View.builder(ContainerTypes.GENERIC_9X3.get())
                .title(title)
                .build(CrateCrate.get().getContainer())
                .define(Layout.builder(3, 9)
                        .set(MENU_LAYOUT.getElements())
                        .set(elements)
                        .build());
    }

    public static Page page(Component title, List<Element> elements, Element back) {

        return Page.builder(ContainerTypes.GENERIC_9X6.get())
                .title(c -> title)
                .layout(PAGE_LAYOUT)
                .icon(BACK, c -> back)
                .build(CrateCrate.get().getContainer())
                .define(elements);
    }

    public static ItemStack item(ItemType type, Component name) {
        return ItemStack.builder()
                .itemType(type)
                .add(Keys.DISPLAY_NAME, name)
                .build();
    }

    public static ItemStack item(ItemType type) {
        return item(type, Component.empty());
    }
}