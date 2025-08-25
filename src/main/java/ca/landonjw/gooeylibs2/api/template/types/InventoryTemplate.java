/*
 * GooeyLibs
 * Copyright (C) 201x - 2024 landonjw
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package ca.landonjw.gooeylibs2.api.template.types;

import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.InventoryListenerButton;
import ca.landonjw.gooeylibs2.api.helpers.TemplateHelper;
import ca.landonjw.gooeylibs2.api.template.LineType;
import ca.landonjw.gooeylibs2.api.template.slot.TemplateSlotDelegate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InventoryTemplate extends ChestTemplate {

    protected InventoryTemplate(@NonNull TemplateSlotDelegate[] slots) {
        super(slots);
    }

    public ItemStack getDisplayForSlot(@NonNull ServerPlayer player, int index) {
        Optional<Button> slot = this.getSlot(index).getButton();
        if (slot.isPresent() && slot.get() instanceof InventoryListenerButton) {
            return player.inventory().slot(index + 9).map(Inventory::poll).map(InventoryTransactionResult.Poll::polledItem).map(ItemStackLike::asMutable).orElseGet(ItemStack::empty);
        }
        else if (slot.isPresent()) {
            return slot.get().getDisplay();
        }
        else {
            return ItemStack.empty();
        }
    }

    @Override
    public InventoryTemplate clone() {
        TemplateSlotDelegate[] references = new TemplateSlotDelegate[this.getSize()];
        for (int i = 0; i < getSize(); i++) {
            int row = i / 9;
            int col = i % 9;

            Optional<Button> button = this.getSlot(i).getButton();
            references[i] = new TemplateSlotDelegate(button.orElse(null), col + row * 9);
        }
        return new InventoryTemplate(references);
    }

    @Deprecated
    public List<ItemStack> getFullDisplay(@NonNull ServerPlayer player) {
        List<ItemStack> displays = new ArrayList<>();

        int PLAYER_INVENTORY_OFFSET = 8;
        for (int i = 0; i <= PLAYER_INVENTORY_OFFSET; i++) displays.add(ItemStack.empty());
        for (int i = 0; i < getSize(); i++) {
            displays.add(getDisplayForSlot(player, i));
        }
        return displays;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /**
         * Instance of the template being built.
         * <p>
         * In 2.1.0, all template builders were moved from storing data instances (ie. button arrays)
         * of templates and constructing them on build, to simply delegating to an instance of the template.
         * <p>
         * This was done in order to shift all convenience methods (ie. row, column, border, etc.) into the
         * corresponding Template classes themselves to allow for easier modification of button state after
         * the Template was built.
         * <p>
         * Since we assign a new template instance at the end of each {@link #build()},
         * this should not have any side effects and thus be backwards compatible.
         * <p>
         * Yay for abstraction!
         */
        private InventoryTemplate templateInstance;

        public Builder() {
            this.templateInstance = new InventoryTemplate(TemplateHelper.slotsOf(4 * COLUMNS));
        }

        public Builder set(int index, @Nullable Button button) {
            templateInstance.set(index, button);
            return this;
        }

        public Builder set(int row, int col, @Nullable Button button) {
            templateInstance.set(row, col, button);
            return this;
        }

        public Builder row(int row, @Nullable Button button) {
            templateInstance.row(row, button);
            return this;
        }

        public Builder rowFromList(int row, @NonNull List<Button> buttons) {
            templateInstance.rowFromList(row, buttons);
            return this;
        }

        public Builder column(int col, @Nullable Button button) {
            templateInstance.column(col, button);
            return this;
        }

        public Builder columnFromList(int col, @NonNull List<Button> buttons) {
            templateInstance.columnFromList(col, buttons);
            return this;
        }

        public Builder line(@NonNull LineType lineType, int startRow, int startCol, int length, @Nullable Button button) {
            templateInstance.line(lineType, startRow, startCol, length, button);
            return this;
        }

        public Builder lineFromList(@NonNull LineType lineType, int startRow, int startCol, int length, @NonNull List<Button> buttons) {
            templateInstance.lineFromList(lineType, startRow, startCol, length, buttons);
            return this;
        }

        public Builder square(int startRow, int startCol, int size, @Nullable Button button) {
            templateInstance.square(startRow, startCol, size, button);
            return this;
        }

        public Builder squareFromList(int startRow, int startCol, int size, @NonNull List<Button> buttons) {
            templateInstance.squareFromList(startRow, startCol, size, buttons);
            return this;
        }

        public Builder rectangle(int startRow, int startCol, int length, int width, @Nullable Button button) {
            templateInstance.rectangle(startRow, startCol, length, width, button);
            return this;
        }

        public Builder rectangleFromList(int startRow, int startCol, int length, int width, @NonNull List<Button> buttons) {
            templateInstance.rectangleFromList(startRow, startCol, length, width, buttons);
            return this;
        }

        public Builder border(int startRow, int startCol, int length, int width, @Nullable Button button) {
            templateInstance.border(startRow, startCol, length, width, button);
            return this;
        }

        public Builder borderFromList(int startRow, int startCol, int length, int width, @NonNull List<Button> buttons) {
            templateInstance.borderFromList(startRow, startCol, length, width, buttons);
            return this;
        }

        public Builder checker(int startRow, int startCol, int length, int width, @Nullable Button button, @Nullable Button button2) {
            templateInstance.checker(startRow, startCol, length, width, button, button2);
            return this;
        }

        public Builder checkerFromList(int startRow, int startCol, int length, int width, @NonNull List<Button> buttons, @NonNull List<Button> buttons2) {
            templateInstance.checkerFromList(startRow, startCol, length, width, buttons, buttons2);
            return this;
        }

        public Builder fill(@Nullable Button button) {
            templateInstance.fill(button);
            return this;
        }

        public Builder fillFromList(@NonNull List<Button> buttons) {
            templateInstance.fillFromList(buttons);
            return this;
        }

        public InventoryTemplate build() {
            InventoryTemplate templateToReturn = templateInstance;
            templateInstance = new InventoryTemplate(TemplateHelper.slotsOf(4 * COLUMNS));
            return templateToReturn;
        }

    }

}