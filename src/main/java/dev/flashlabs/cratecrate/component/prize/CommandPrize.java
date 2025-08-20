package dev.flashlabs.cratecrate.component.prize;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.DisplayItem;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.component.effect.Effect;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Serializers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.text.WordUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Optional;

public final class CommandPrize extends Prize<String> {

    public static final Type<CommandPrize> TYPE = new CommandPrizeType();

    private enum Source {
        SERVER, PLAYER
    }

    private final String command;
    private final Source source;
    private final boolean online;

    private CommandPrize(
        String id,
        DisplayItem displayItem,
        String command,
        Source source,
        boolean online
    ) {
        super(id, displayItem);
        this.command = command;
        this.source = source;
        this.online = online;
    }

    /**
     * Returns the name of this prize, defaulting to the command prefixed with
     * {@code '/'}. If a reference value is given, it replaces {@code ${value}}.
     * If a reference value is given, it replaces {@code ${value}}.
     */
    @Override
    public Component name(Optional<String> value) {
        var base = displayItem().name().orElseGet(() -> id.startsWith("/") ? id : WordUtils.capitalize(id.replace("-", " ")));
        base = base.replaceAll("\\$\\{value}", value.orElse("${value}"));
        return LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + base);
    }

    /**

     * Returns the lore of this prize, defaulting to an empty list if this prize
     * is an inline reference else the command (prefixed with {@code '/'}). If a
     * reference value is given, it replaces {@code ${value}}.
     */
    @Override
    public List<Component> lore(Optional<String> value) {
        return Optional.of(displayItem().lore()).filter(List::isEmpty)
                .orElse(id.startsWith("/") ? List.of() : List.of("/" + command)).stream()

                .map(s -> {
            s = s.replaceAll("\\$\\{value}", value.orElse("${value}"));
            return LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + s).asComponent();
        }).toList();
    }

    /**
     * Returns the icon of this prize, defaulting to a filled map. If the icon
     * does not have a defined display name or lore, it is set to this prize's
     * name/lore.
     */
    @Override
    public ItemStack icon(Optional<String> value) {
        var base = displayItem().icon().map(ItemStackSnapshot::asMutable)
            .orElseGet(() -> ItemStack.of(ItemTypes.FILLED_MAP, 1));
        if (base.get(Keys.CUSTOM_NAME).isEmpty()) {
            base.offer(Keys.CUSTOM_NAME, name(value));
        }
        if (base.get(Keys.LORE).isEmpty()) {
            base.offer(Keys.LORE, lore(value));
        }
        return base;
    }

    @Override
    public boolean give(User user, String value) {
        try (var frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            var command = this.command.replaceAll("\\$\\{value}", value);
            if (online || source == Source.PLAYER) {
                var player = user.player().orElseThrow(() -> new CommandException(Component.text("User must be online.")));
                frame.pushCause(source == Source.PLAYER ? player : Sponge.systemSubject());
                command = command.replaceAll("\\$\\{player}", player.name());
            } else {
                frame.pushCause(Sponge.systemSubject());
                command = command.replaceAll("\\$\\{user}", user.name());
            }
            System.out.println(command);
            Sponge.server().commandManager().process(command);
            return true;
        } catch (CommandException e) {
            CrateCrate.get().logger().error("Error processing command: ", e);
            return false;
        }
    }

    private static final class CommandPrizeType extends Type<CommandPrize> {

        private CommandPrizeType() {
            super("Command", CrateCrate.get().getContainer());
        }

        /**
         * Deserializes a command prize, defined as:
         *
         * <pre>{@code
         * CommandPrize:
         *     name: Optional<String>
         *     lore: Optional<List<String>>
         *     icon: Optional<ItemStack>
         *     command: String (prefixed with '/') | Object
         *         command: String (prefixed with '/')
         *         source: Optional<Source>
         *         online: Optional<Boolean>
         * }</pre>
         */
        @Override
        public CommandPrize deserializeComponent(String id, ConfigurationNode node) throws SerializationException {
            var displayItem = DisplayItem.deserialize(node);

            var commandNode = node.node("command");

            var command = "";
            var source = Source.SERVER;
            var online = false;

            if(commandNode.isMap()) {
                command = Optional.ofNullable(commandNode.node("command").getString()).map(s -> s.substring(1)).orElse("");
                source = Optional.ofNullable(commandNode.node("source").getString()).map(String::toUpperCase).map(Source::valueOf).orElse(Source.SERVER);
                online = commandNode.node("online").getBoolean();
            } else {
                command = Optional.ofNullable(commandNode.getString()).map(s -> s.substring(1)).orElse("");
            }



            return new CommandPrize(id, displayItem, command, source, online);
        }

        /**
         * Deserializes a command prize reference, defined as:
         *
         * <pre>{@code
         * CommandPrizeReference:
         *     node:
         *        CommandPrize |
         *        String (CommandPrize id or prefixed with '/')
         *     values: [
         *        Optional<String> (only allowed with String CommandPrize id)
         *     ]
         * }</pre>
         */
        @Override
        public PrizeValueHolder<CommandPrize, ?> deserializeReference(ConfigurationNode node) throws SerializationException {
            CommandPrize prize;
            if (node.isMap()) {
                prize = deserializeComponent("CommandPrize@" + node.path(), node);
                Config.PRIZES.put(prize.id, prize);
            } else {
                var identifier = Optional.ofNullable(node.getString()).orElse("");
                if (Config.PRIZES.containsKey(identifier)) {
                    prize = (CommandPrize) Config.PRIZES.get(identifier);
                } else if (identifier.startsWith("/")) {
                    prize = new CommandPrize(identifier, new DisplayItem(), identifier.substring(1), Source.SERVER, false);
                    Config.PRIZES.put(prize.id, prize);
                } else {
                    throw new AssertionError(identifier);
                }
            }
            //TODO: Validate reference value counts
            var value = node.node("value").getString("");
            return new PrizeValueHolder<>(prize, value);
        }

    }

}
