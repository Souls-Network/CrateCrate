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

package ca.landonjw.gooeylibs2.api.button.linked;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.function.Consumer;

public class LinkedPageButton extends GooeyButton {

    private LinkType linkType;

    protected LinkedPageButton(@NonNull ItemStack display,
                               @Nullable Consumer<ButtonAction> onClick,
                               @NonNull LinkType linkType) {
        super(display, onClick);
        this.linkType = linkType;
    }

    @Override
    public void onClick(@NonNull ButtonAction action) {
        super.onClick(action);
        if (action.getPage() instanceof LinkedPage) {
            LinkedPage linkedPage = (LinkedPage) action.getPage();
            Page targetPage = (linkType == LinkType.Previous) ? linkedPage.getPrevious() : linkedPage.getNext();
            if (targetPage != null) {
                UIManager.openUIForcefully(action.getPlayer(), targetPage);
            }
        }
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends GooeyButton.Builder {

        private LinkType linkType;

        @Override
        public Builder display(@NonNull ItemStack display) {
            super.display(display);
            return this;
        }

        @Override
        public Builder onClick(@Nullable Runnable behaviour) {
            super.onClick(behaviour);
            return this;
        }

        @Override
        public Builder onClick(@Nullable Consumer<ButtonAction> behaviour) {
            super.onClick(behaviour);
            return this;
        }

        public Builder linkType(@NonNull LinkType linkType) {
            this.linkType = linkType;
            return this;
        }

        @Override
        public <T extends Value<V>, V> Builder with(Key<T> type, V value) {
            super.with(type, value);
            return this;
        }

        @Override
        public LinkedPageButton build() {
            validate();
            return new LinkedPageButton(this.display, onClick, linkType);
        }

        @Override
        protected void validate() {
            super.validate();
            if (linkType == null) {
                throw new IllegalStateException("link type must be defined!");
            }
        }
    }

}