package dev.flashlabs.cratecrate.component.prize;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Type;
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

    public static final Type<CommandPrize, String> TYPE = new CommandPrizeType();

    private enum Source {
        SERVER, PLAYER
    }

    private final Optional<String> name;
    private final Optional<List<String>> lore;
    private final Optional<ItemStackSnapshot> icon;
    private final String command;
    private final Optional<Source> source;
    private final Optional<Boolean> online;

    private CommandPrize(
        String id,
        Optional<String> name,
        Optional<List<String>> lore,
        Optional<ItemStackSnapshot> icon,
        String command,
        Optional<Source> source,
        Optional<Boolean> online
    ) {
        super(id);
        this.name = name;
        this.lore = lore;
        this.icon = icon;
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
        var base = name.orElseGet(() -> id.startsWith("/") ? id : WordUtils.capitalize(id.replace("-", " ")));
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
        return lore
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
        var base = icon.map(ItemStackSnapshot::asMutable)
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
            if (online.orElse(false) || source.map(s -> s == Source.PLAYER).orElse(false)) {
                var player = user.player().orElseThrow(() -> new CommandException(Component.text("User must be online.")));
                frame.pushCause(source.map(s -> s == Source.PLAYER).orElse(false) ? player : Sponge.systemSubject());
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

    private static final class CommandPrizeType extends Type<CommandPrize, String> {

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
        public CommandPrize deserializeComponent(ConfigurationNode node) throws SerializationException {
            var name = Optional.ofNullable(node.node("name").get(String.class));
            var lore = node.node("lore").isList()
                ? Optional.ofNullable(node.node("lore").getList(String.class)).map(List::copyOf)
                : Optional.<List<String>>empty();
            var icon = node.hasChild("icon")
                ? Optional.of(Serializers.ITEM_STACK.deserialize(node.node("icon")).asImmutable())
                : Optional.<ItemStackSnapshot>empty();
            var command = Optional.ofNullable(node.node("command").getString())
                .or(() -> Optional.ofNullable(node.node("command", "command").getString()))
                .map(s -> s.substring(1))
                .orElse("");
            var source = Optional.ofNullable(node.node("command", "source").getString())
                .map(s -> Source.valueOf(s.toUpperCase()));
            var online = Optional.ofNullable(node.node("command", "online").getString())
                .map(Boolean::parseBoolean);
            return new CommandPrize(String.valueOf(node.key()), name, lore, icon, command, source, online);
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
        public Tuple<CommandPrize, String> deserializeReference(ConfigurationNode node, List<? extends ConfigurationNode> values) throws SerializationException {
            CommandPrize prize;
            if (node.isMap()) {
                prize = deserializeComponent(node);
                Config.PRIZES.put(prize.id, prize);
            } else {
                var identifier = Optional.ofNullable(node.getString()).orElse("");
                if (Config.PRIZES.containsKey(identifier)) {
                    prize = (CommandPrize) Config.PRIZES.get(identifier);
                } else if (identifier.startsWith("/")) {
                    prize = new CommandPrize(identifier, Optional.empty(), Optional.empty(), Optional.empty(), identifier.substring(1), Optional.empty(), Optional.empty());
                    Config.PRIZES.put(prize.id, prize);
                } else {
                    throw new AssertionError(identifier);
                }
            }
            //TODO: Validate reference value counts
            var value = Optional.ofNullable((!values.isEmpty() ? values.getFirst() : node.node("value")).getString()).orElse("");
            return Tuple.of(prize, value);
        }

    }

}
