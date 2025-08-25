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

package ca.landonjw.gooeylibs2.api.template;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.item.inventory.ContainerType;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.registry.DefaultedRegistryReference;

import java.util.function.Function;

public enum TemplateType {
    CHEST(template -> {
        return switch (template.getSize() / 9) {
            case 1 -> ContainerTypes.GENERIC_9X1;
            case 2 -> ContainerTypes.GENERIC_9X2;
            case 3 -> ContainerTypes.GENERIC_9X3;
            case 4 -> ContainerTypes.GENERIC_9X4;
            case 5 -> ContainerTypes.GENERIC_9X5;
            default -> ContainerTypes.GENERIC_9X6;
        };
    }),
    FURNACE(template -> ContainerTypes.FURNACE),
    BREWING_STAND(template -> ContainerTypes.BREWING_STAND),
    HOPPER(template -> ContainerTypes.HOPPER),
    DISPENSER(template -> ContainerTypes.GENERIC_3X3),
    CRAFTING_TABLE(template -> ContainerTypes.CRAFTING);

    private final Function<Template, DefaultedRegistryReference<ContainerType>> containerTypeSupplier;

    TemplateType(@NonNull Function<Template, DefaultedRegistryReference<ContainerType>> containerTypeSupplier) {
        this.containerTypeSupplier = containerTypeSupplier;
    }

    public ContainerType getContainerType(@NonNull Template template) {
        return containerTypeSupplier.apply(template).get();
    }

}