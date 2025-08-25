package ca.landonjw.gooeylibs2.api.container;

import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.moveable.Movable;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.ButtonClick;
import ca.landonjw.gooeylibs2.api.template.Template;
import ca.landonjw.gooeylibs2.api.template.types.InventoryTemplate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.item.inventory.*;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.util.Ticks;

import java.util.*;

/**
 * Sponge API 12 port of GooeyContainer.
 * This version drops dependency on net.minecraft.* and uses Sponge's InventoryMenu/ViewableInventory.
 *
 * Public API preserved (constructor, {@link #open()}, {@link #refresh()}, {@link #getPage()}, {@link #setCarriedButton(Button)}).
 * Internals are reimplemented using Sponge events/callbacks.
 */
public final class GooeyContainer {

    private final ServerPlayer player;
    private final Page page;
    private final InventoryTemplate inventoryTemplate; // player inv template used for SHIFT/number-press handling

    private ViewableInventory viewable;
    private InventoryMenu menu;
    private Container openContainer; // present while open
    private Button cursorButton; // for Movable handling
    private boolean closing;
    private Ticks lastClickTick;

    // index -> Button mapping for current page
    private final Map<Integer, Button> buttonMap = new HashMap<>();

    public GooeyContainer(@NonNull ServerPlayer player, @NonNull Page page) {
        this.player = Objects.requireNonNull(player, "player");
        this.page = Objects.requireNonNull(page, "page");
        this.inventoryTemplate = page.getInventoryTemplate().orElseThrow();

        // Build initial view based on template size (default to generic 9xN chest menus)
        int templateSize = this.page.getTemplate().getSize();
        int rows = Math.max(1, Math.min(6, (templateSize + 8) / 9));
        ContainerType type = resolveContainerType(rows);

        this.viewable = ViewableInventory.builder()
                .type(type)
                .completeStructure()
                .build();

        this.menu = InventoryMenu.of(this.viewable);
        this.menu.setReadOnly(false); // we will manage click cancellation ourselves

        // Title: best-effort; callers may change via page bindings

        this.menu.setReadOnly(true);

        bindPage();
        registerHandlers();
    }

    /** Opens the menu for the player. */
    public void open() {
        Optional<Container> opened = this.menu.open(this.player);
        this.openContainer = opened.orElse(null);
    }

    /** Re-renders the current page into the container. */
    public void refresh() {
        if (this.closing) return;
        bindPage();
        // force re-render of items
        renderAll(false);
    }

    /** For compatibility with existing code that checked validity in NMS containers. */
    public boolean stillValid(@NonNull Object ignored) {
        return !this.closing && (this.openContainer == null || this.openContainer.isOpen());
    }

    /** Called when menu is closed. */
    public void removed() {
        this.closing = true;
        if (this.menu != null) {
            this.menu.unregisterAll();
        }
        this.buttonMap.clear();
        this.openContainer = null;
        this.cursorButton = null;
    }

    public @NonNull Page getPage() {
        return this.page;
    }

    public void setCarriedButton(Button button) {
        this.cursorButton = button;
        setPlayersCursor((button == null) ? ItemStack.empty() : convertDisplay(button.getDisplay()));
    }

    // ===== Internal building and handlers =====

    private void bindPage() {
        // swap container type if template changed rows
        int templateSize = this.page.getTemplate().getSize();
        int rows = Math.max(1, Math.min(6, (templateSize + 8) / 9));
        ContainerType desired = resolveContainerType(rows);

        if (this.viewable == null) {
            this.viewable = ViewableInventory.builder().type(desired).completeStructure().build();
            this.menu.setCurrentInventory(this.viewable);
        } else {
            // if type differs, rebuild
            // Note: setCurrentInventory will close/reopen the container if type changes.
            this.viewable = ViewableInventory.builder().type(desired).completeStructure().build();
            this.menu.setCurrentInventory(this.viewable);
        }

        // Title
        this.menu.setTitle(this.page.getTitle());

        // Build index -> button cache
        this.buttonMap.clear();
        Template template = this.page.getTemplate();
        for (int i = 0; i < template.getSize(); i++) {
            Button b = template.getSlot(i).getButton().orElse(null);
            if (b != null) {
                this.buttonMap.put(i, b);
            }
        }

        // Initial render
        renderAll(true);
    }

    private void renderAll(boolean initial) {
        if (this.viewable == null) return;
        List<Slot> slots = this.viewable.slots();
        Template template = this.page.getTemplate();

        for (int i = 0; i < template.getSize(); i++) {
            ItemStackLike stack = ItemStack.empty();
            Button b = this.buttonMap.get(i);
            if (b != null) {
                stack = convertDisplay(b.getDisplay());
            }
            if (i < slots.size()) {
                setSlotStack(slots.get(i), stack, initial);
            }
        }
    }

    private void registerHandlers() {
        // Basic click handler without slot (e.g., drag end etc.): cancel by default
        this.menu.registerClick((Cause cause, Container container, ClickType<?> clickType) -> {
            this.lastClickTick = Sponge.server().runningTimeTicks();
            // Allow default; more detailed handling happens in slot-click
            return true;
        });

        // Slot click handler mirrors original "clicked(int slot, int dragType, ClickType, Player)"
        this.menu.registerSlotClick((Cause cause, Container container, Slot slot, int slotIndex, ClickType<?> clickType) -> {
            this.lastClickTick = Sponge.server().runningTimeTicks();
            return handleSlotClick(cause, container, slotIndex, clickType);
        });

        this.menu.registerKeySwap((Cause cause, Container container, Slot slot, int slotIndex, ClickType<?> clickType, Slot slot2) -> {
            // emulate ClickType.SWAP with dragType = targetHotbar
            // We don't know which slot was under cursor; ignore and allow
            return true;
        });

        this.menu.registerChange((Cause cause, Container container, Slot slot, int slotIndex, ItemStackSnapshot oldStack, ItemStackSnapshot newStack) -> {
            // Default allow; Movable logic will override via cursorButton when needed.
            return true;
        });

        this.menu.registerClose((Cause cause, Container container) -> {
            removed();
        });
    }

    private boolean handleSlotClick(Cause cause, Container container, int slotIndex, ClickType<?> clickType) {
        // Ignore invalid
        if (slotIndex < 0) {
            if (this.cursorButton != null) {
                setPlayersCursor(convertDisplay(this.cursorButton.getDisplay()));
            }
            return false;
        }

        // Desync patch equivalent: keep cursor in sync if we track a movable
        patchDesyncs(slotIndex, clickType);

        Button button = getButton(slotIndex);

        if (button instanceof Movable || this.cursorButton != null) {
            // handle movable drag/pick/place semantics
            // NOTE: Detailed parity with NMS QUICK_CRAFT semantics is non-trivial.
            // We implement simplified behaviour:
            if (this.cursorButton == null && button != null && button instanceof Movable) {
                // pick up
                this.cursorButton = button;
                setPlayersCursor(convertDisplay(button.getDisplay()));
                updateSlotStack(slotIndex, ItemStack.empty(), false);
                this.buttonMap.remove(slotIndex);
            } else {
                // place
                Button toPlace = this.cursorButton;
                if (toPlace != null) {
                    this.buttonMap.put(slotIndex, toPlace);
                    updateSlotStack(slotIndex, convertDisplay(toPlace.getDisplay()), false);
                    this.cursorButton = null;
                    setPlayersCursor(ItemStack.empty());
                    invokeButtonClick(toPlace, slotIndex, ButtonClick.LEFT_CLICK);
                }
            }
            return false;
        }

        // Non-movable: force empty cursor
        setPlayersCursor(ItemStack.empty());

        // SHIFT handling: emulate quick-move into player inventory template
//        if (clickType == ClickTypes.QUICK_MOVE.get()) {
//            // Let Sponge move items; we ensure display is restored next render
//            updateSlotStack(slotIndex, getItemAtSlot(slotIndex), false);
//        }

        // Invoke action
        if (button != null) {
            ButtonClick click = mapClickType(clickType);
            ButtonAction action = new ButtonAction(this.player, click, button, page.getTemplate(), page, slotIndex);
            button.onClick(action);
        }
        return false;
    }

    private void patchDesyncs(int slot, ClickType<?> type) {
        if (this.openContainer == null) return;
        // Keep cursor display consistent with our tracked cursorButton
        setPlayersCursor((this.cursorButton != null) ? convertDisplay(this.cursorButton.getDisplay()) : ItemStack.empty());
    }

    private void invokeButtonClick(Button button, int slotIndex, ButtonClick click) {
        if (button == null) {
            return;
        }
        ButtonAction action = new ButtonAction(
                this.player,
                click,
                button,
                this.page.getTemplate(),
                this.page,
                slotIndex
        );
        button.onClick(action);
    }

    private Button getButton(int slot) {
        return this.buttonMap.get(slot);
    }

    private void setPlayersCursor(ItemStackLike stack) {
        if (this.openContainer != null) {
            this.openContainer.setCursor(stack);
        } else {
            // if not open yet, stash via cursorButton only
        }
    }

    private ItemStackLike getItemAtSlot(int slot) {
        List<Slot> slots = this.viewable.slots();
        if (slot >= 0 && slot < slots.size()) {
            return slots.get(slot).peek();
        }
        return ItemStack.empty();
    }

    private void updateSlotStack(int slotIndex, ItemStackLike stack, boolean silent) {
        List<Slot> slots = this.viewable.slots();
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            setSlotStack(slots.get(slotIndex), stack, silent);
        }
    }

    private void setSlotStack(Slot slot, ItemStackLike stack, boolean silent) {
        // Sponge inventories are readonly by default through InventoryMenu; we are allowed to set programmatically.
        slot.set(stack);
    }

    private ButtonClick mapClickType(ClickType<?> type) {
        if (type == ClickTypes.CLICK_LEFT.get()) return ButtonClick.LEFT_CLICK;
        if (type == ClickTypes.CLICK_RIGHT.get()) return ButtonClick.RIGHT_CLICK;
        if (type == ClickTypes.CLICK_MIDDLE.get()) return ButtonClick.MIDDLE_CLICK;
        if (type == ClickTypes.SHIFT_CLICK_LEFT.get()) return ButtonClick.SHIFT_LEFT_CLICK;
        if (type == ClickTypes.SHIFT_CLICK_RIGHT.get()) return ButtonClick.SHIFT_RIGHT_CLICK;
        if (type == ClickTypes.KEY_THROW_ONE.get() || type == ClickTypes.KEY_THROW_ALL.get()) return ButtonClick.THROW;
        return ButtonClick.OTHER;
    }

    private ContainerType resolveContainerType(int rows) {
        // Best-effort mapping to chest-like container types.
        // Adjust if your Sponge API names differ.
        return switch (rows) {
            case 1 -> ContainerTypes.GENERIC_9X1.get();
            case 2 -> ContainerTypes.GENERIC_9X2.get();
            case 3 -> ContainerTypes.GENERIC_9X3.get();
            case 4 -> ContainerTypes.GENERIC_9X4.get();
            case 5 -> ContainerTypes.GENERIC_9X5.get();
            default -> ContainerTypes.GENERIC_9X6.get();
        };
    }

    // ===== Conversion helpers =====

    /**
     * Convert the platform-specific display stack from Button (likely a Mojang ItemStack)
     * into a Sponge API {@link ItemStackLike}. This MUST be implemented for your platform bridge.
     * By default, this throws to force you to provide an adapter.
     */
    private ItemStackLike convertDisplay(Object platformDisplay) {
        if (platformDisplay instanceof ItemStackLike like) {
            return like;
        }
        if (platformDisplay instanceof ItemStack spongeStack) {
            return spongeStack;
        }
        // TODO: Implement conversion from net.minecraft.world.item.ItemStack -> Sponge ItemStack
        throw new UnsupportedOperationException("Implement display ItemStack conversion for Sponge (received: " + platformDisplay + ")");
    }
}
