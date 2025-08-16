package dev.flashlabs.cratecrate.command;

import dev.flashlabs.cratecrate.CrateCrate;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import java.net.MalformedURLException;
import java.net.URL;

import static net.kyori.adventure.text.Component.text;

public final class CommandUtils {

    private static final Component FOOTER = Component.join(JoinConfiguration.separator(text(" | ", NamedTextColor.GRAY)),
        link("Ore", "https://ore.spongepowered.org/FlashLabs/CrateCrate"),
        link("Source", "https://github.com/Flash-Labs/CrateCrate"),
        link("Discord", "https://discord.gg/zWqnAa9KRn"),
        link("FlashLabs", "https://flashlabs.dev")
    );

    public static Component usage(String base, String description, Component... arguments) {
        return text(base)
                .color(NamedTextColor.GOLD)
                .hoverEvent(text(description, NamedTextColor.GRAY))
                .clickEvent(arguments.length == 0 ? ClickEvent.runCommand(base) : ClickEvent.suggestCommand(base))
            .append(Component.join(JoinConfiguration.separator(text(" ")), arguments));
    }

    public static Component argument(String name, boolean required, String description) {
        return text((required ? "<" : "[") + name + (required ? ">" : "]"))
            .color(required ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
                .hoverEvent(text(description, NamedTextColor.GRAY));
    }

    public static Component link(String name, String url) {
        try {
            return text(name)
                .color(NamedTextColor.WHITE)
                .hoverEvent(text(url, NamedTextColor.GRAY))
                .clickEvent(ClickEvent.openUrl(new URL(url)));
        } catch (MalformedURLException ignored) {
            return text(name)
                .color(NamedTextColor.WHITE)
                .hoverEvent(text(url, NamedTextColor.RED));
        }
    }

    public static void paginate(Audience src, Component... contents) {
        PaginationList.builder()
            .title(LinearComponents.linear(
                NamedTextColor.YELLOW, text("Crate"),
                NamedTextColor.GOLD, text("Crate"),
                NamedTextColor.WHITE, text(" v" + CrateCrate.get().getContainer().metadata().version().toString())
            ))
            .padding(text("=", NamedTextColor.GRAY))
            .contents(contents)
            .footer(LinearComponents.linear(text(src instanceof Player ? "                   " : "          "), FOOTER))
            .sendTo(src);
    }

}