package dev.flashlabs.flashlibs.plugin;

import dev.flashlabs.flashlibs.message.MessageService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.util.locale.LocaleSource;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * A superclass for plugin instances that manages common resources such as
 * messages. This class assumes information about the location of assets,
 * configuration files, and other structural decisions.
 *
 * <p>The provided {@link MessageService} assumes the base messages config is
 * the asset {@code messages/messages.conf} and files are stored in the
 * {@code messages} folder within the plugin directory.</p>
 */
public abstract class PluginInstance {

    protected final PluginContainer container;
    protected final Logger logger;
    protected final Path directory;
    protected final MessageService messages;

    /**
     * Creates a new PluginInstance using the given container, which should be
     * obtained through constructor injection in the subclass.
     */
    protected PluginInstance(PluginContainer container) {
        this.container = container;
        logger = container.logger();
        directory = Sponge.configManager().pluginConfig(container).directory();
        try {
            Path path = Files.createDirectories(directory.resolve("messages"));
            var stream = container.openResource("messages/messages.conf");

            if(stream.isPresent()) {
                Files.copy(stream.get(), path);
            }

            messages = MessageService.of("messages", path);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public final PluginContainer getContainer() {
        return container;
    }

    public final Logger logger() {
        return logger;
    }

    /**
     * @see MessageService#get(String, Locale, Object...)
     */
    public final Component getMessage(String key, Locale locale, Object... args) {
        return messages.get(key, locale, args);
    }

    /**
     * @see MessageService#send(T, Identity, String, Object...)
     */
    public final <T extends Audience & LocaleSource> void sendMessage(T source, String key, Object... args) {
        messages.send(source, Identity.nil(), key, args);
    }
}