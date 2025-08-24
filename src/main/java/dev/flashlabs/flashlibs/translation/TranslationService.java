package dev.flashlabs.flashlibs.translation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides an interface for reading translations from {@link ResourceBundle}s
 * through the provided class loader and name. Translations may be formatted as
 * property files as well as in Hocon (.conf), Json (.json), and Yaml (.yaml).
 *
 * @see ResourceBundle
 */
public final class TranslationService {

    private final String name;
    private final Path path;
    private final Map<Locale, ResourceBundle> cache = new ConcurrentHashMap<>();

    private TranslationService(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    /**
     * Creates a service loading resource bundles using the given name and path.
     *
     * @throws MalformedURLException If the path's URL could not be created.
     */
    public static TranslationService of(String name, Path path) throws MalformedURLException {
        return new TranslationService(name, path);
    }

    /**
     * Returns a {@link ResourceBundle} for the given locale.
     *
     * @see ResourceBundle#getBundle(String, Locale)
     * @throws java.util.MissingResourceException If no resource bundle exists
     */
    public ResourceBundle getBundle(Locale locale) {
        return cache.computeIfAbsent(locale, this::loadBundle);    }

    /**
     * Returns a string value retrieved through the bundle for the given locale.
     * If the given key is not present, the key itself is returned.
     */
    public String getString(String key, Locale locale) {
        ResourceBundle bundle = getBundle(locale);
        return bundle.containsKey(key) ? bundle.getObject(key).toString() : key;
    }

    /**
     * Reloads the resource bundle cache corresponding to this class loader.
     */
    public void reload() {
        cache.clear();
    }

    private ResourceBundle loadBundle(Locale locale) {
        try {
            // build candidate like messages_en, messages

            var bundle = getBundleExtra(locale);

            if(bundle == null) bundle = getBundleExtra(null);

            if(bundle == null) throw new MissingResourceException("Bundle not found", name, locale.toString());

            return bundle;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceBundle getBundleExtra(@Nullable Locale locale) throws IOException {
        String base = name + (locale == null || locale.toString().isEmpty() ? "" : "_" + locale);

        for (String ext : List.of("conf", "json", "yaml", "properties")) {
            String res = base.replace('.', '/') + "." + ext;

            Path url = path.resolve(res);
            System.out.println("BUNDLE: " + url);

            if(Files.notExists(url)) continue;

            return switch (ext) {
                case "conf" -> new NodeResourceBundle(HoconConfigurationLoader.builder().path(url).build().load());
                case "json" -> new NodeResourceBundle(GsonConfigurationLoader.builder().path(url).build().load());
                case "yaml" -> new NodeResourceBundle(YamlConfigurationLoader.builder().path(url).build().load());
                case "properties" -> {
                    try (var r = Files.newInputStream(url)) {
                        yield new PropertyResourceBundle(r);
                    }
                }
                default -> throw new IllegalStateException("Unexpected ext " + ext);
            };
        }

        return null;
    }
}



