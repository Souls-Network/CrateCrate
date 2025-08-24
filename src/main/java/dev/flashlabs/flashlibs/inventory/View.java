package dev.flashlabs.flashlibs.inventory;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.*;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents an inventory which can be displayed to multiple players. Events
 * that are outside of this inventory (such as the hotbar) will not be canceled.
 */
public final class View {

    private final ViewableInventory inventory;
    private final Map<Integer, Element> elements = new HashMap<>();
    private final PluginContainer container;
    private final InventoryMenu menu;
    private final Consumer<InteractContainerEvent.Close> closeHandler;

    private View(Builder builder, PluginContainer container) {
        inventory = ViewableInventory.builder()
                .type(builder.archetype)
                .fillDummy()
                .item(ItemStackSnapshot.empty())
                .completeStructure()
                .plugin(container).build();
        menu = inventory.asMenu();
        menu.registerSlotClick(View.this::onClick);
        menu.setTitle(builder.title);
        this.container = container;

        this.closeHandler = builder.closeHandler;
    }

    /**
     * Opens this view for the player.
     */
    public void open(ServerPlayer player) {
        menu.open(player).ifPresent(container -> Sponge.eventManager().registerListeners(View.this.container, new ViewListener(container)));
    }

    /**
     * Sets all elements in the given layout to the corresponding slot in the
     * inventory. Undefined indices are considered to be {@link Element#EMPTY}.
     */
    public View define(Layout layout) {
        for (int i = 0; i < inventory.capacity(); i++) {
            System.out.println("BLARGE! " + i +" " + layout.getElements().getOrDefault(i, Element.EMPTY));
            set(layout.getElements().getOrDefault(i, Element.EMPTY), i);
        }

        return this;
    }

    /**
     * Sets all elements in the given layout to the corresponding slot in the
     * inventory. Undefined indices will be left unchanged.
     */
    public View update(Layout layout) {
        layout.getElements().forEach((i, e) -> set(e, i));
        return this;
    }

    /**
     * Sets the element for a slot, modifying the display item and click action.
     */
    public void set(Element element, int index) {
        if (element == Element.EMPTY) {
            elements.remove(index);
        } else {
            elements.put(index, element);
        }
        set(element.getItem().asMutable(), index);
    }

    /**
     * Sets the display item of a slot without modifying the click action.
     */
    public void set(ItemStack item, int index) {
        System.out.println("MERB: " + index + " " + item.type().key(RegistryTypes.ITEM_TYPE) + " " + inventory.set(index, item).type());
    }

    private boolean onClick(Cause cause, Container ignoredContainer, Slot slot, int slotIndex, ClickType<?> clickType) {
        cause.first(ServerPlayer.class).ifPresent(player -> {
            if (elements.containsKey(slotIndex)) {
                elements.get(slotIndex).onClick(new Action.Click(player, this, slot, slotIndex, clickType));
            }
        });

        return false;
    }

    public void execute(Consumer<View> callback) {
        var task = Task.builder().execute(() -> callback.accept(this)).plugin(container).build();
        Sponge.server().scheduler().submit(task);
    }

    /**
     * Creates a new builder for views with the given archetype.
     */
    public static Builder builder(ContainerType archetype) {
        return new Builder(archetype);
    }

    /**
     * A builder for creating {@link View}s.
     */
    public static class Builder {

        private final ContainerType archetype;
        private Component title = Component.empty();
        private Consumer<InteractContainerEvent.Close> closeHandler = event -> {};

        private Builder(ContainerType archetype) {
            this.archetype = archetype;
        }

        /**
         * Sets the title of the inventory. The default is {@link Component#empty()}.
         */
        public Builder title(Component title) {
            this.title = title;
            return this;
        }

//        /**
//         * Adds an action for when the view is opened.
//         */
//        public Builder onOpen(Consumer<Action<InteractInventoryEvent.Open>> onOpen) {
//            listeners.put(InteractInventoryEvent.Open.class, (Consumer<Action>) (Object) onOpen);
//            return this;
//        }

        /**
         * Adds an action for when the view is closed.
         */
        public Builder onClose(Consumer<InteractContainerEvent.Close> onClose) {
            closeHandler = onClose;
            return this;
        }

        /**
         * Creates a View from this builder.
         */
        public View build(PluginContainer container) {
            return new View(this, container);
        }

    }

    private class ViewListener {
        private final Container container;

        public ViewListener(Container container) {
            this.container = container;
        }

        @Listener
        public void onClose(InteractContainerEvent.Close event) {
            if(event.container().equals(container)) {
                View.this.closeHandler.accept(event);
            }
        }
    }
}
