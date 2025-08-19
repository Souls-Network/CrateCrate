package dev.flashlabs.flashlibs.inventory;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

import java.awt.event.ContainerEvent;
import java.util.function.Consumer;

/**
 * Represents an active inventory event connected to a {@link View}, such as
 * opening/closing the inventory or clicking a slot.
 */
public class Action {
    private final ServerPlayer player;
    private final View view;

    Action(ServerPlayer player, View view) {
        this.player = player;
        this.view = view;
    }

    /**
     * Returns the player causing this event.
     */
    public final ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Adds a callback which is executed after this event has completed. The
     * callback is given access to the view linked to this event.
     *
     * Any actions which can cause an inventory event, such as modifying a slot
     * or opening/closing an inventory, must be done through a callback.
     */
    public final void callback(Consumer<View> callback) {
        view.execute(callback);
    }

    /**
     * An action for clicks that provides the slot transaction and index. The
     * incoming event is always cancelled, but can be uncanceled to allow the
     * event to proceed.
     */
    public static final class Click extends Action {

        private final Slot slot;
        private final int index;
        private final ClickType<?> clickType;

        Click(ServerPlayer player, View view, Slot slot, int index, ClickType<?> clickType) {
            super(player, view);
            this.slot = slot;
            this.index = index;
            this.clickType = clickType;
        }

        /**
         * Returns the slot transaction corresponding to this click. The result
         * can only be modified if the event is not cancelled.
         */
        public Slot getSlot() {
            return slot;
        }

        /**
         * Returns the index of the slot corresponding to the linked view.
         */
        public int getIndex() {
            return index;
        }

        public ClickType<?> getClickType() {
            return clickType;
        }
    }

}
