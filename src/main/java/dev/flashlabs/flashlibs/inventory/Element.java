package dev.flashlabs.flashlibs.inventory;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

import java.util.function.Consumer;

/**
 * Represents a slot in an inventory, consisting of a display item and a click
 * action which is executed when the slot is clicked.
 */
public final class Element {

    public static final Element EMPTY = of(ItemStack.empty());

    private final ItemStackSnapshot item;
    private final ClickAction onClick;

    private Element(ItemStack item, ClickAction onClick) {
        this.item = item.createSnapshot();
        this.onClick = onClick;
    }

    /**
     * Creates an Element with the given item and no action.
     */
    public static Element of(ItemStack item) {
        return new Element(item, (clickType, player, container, view, slot, slotIndex) -> {});
    }

    /**
     * Creates an Element with the given item and click action.
     */
    public static Element of(ItemStack item, ClickAction onClick) {
        return new Element(item, onClick);
    }

    public ItemStackSnapshot getItem() {
        return item;
    }

    void onClick(ClickType<?> clickType, ServerPlayer player, Container container, View view, Slot slot, int slotIndex) {
        onClick.onClick(clickType, player, container, view, slot, slotIndex);
    }

    @FunctionalInterface
    public interface ClickAction {
        public void onClick(ClickType<?> clickType, ServerPlayer player, Container container, View view, Slot slot, int slotIndex);
    }
}
