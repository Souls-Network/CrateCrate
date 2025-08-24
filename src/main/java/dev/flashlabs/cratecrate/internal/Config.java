package dev.flashlabs.cratecrate.internal;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.component.Component;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.Reward;
import dev.flashlabs.cratecrate.component.Type;
import dev.flashlabs.cratecrate.component.effect.Effect;
import dev.flashlabs.cratecrate.component.key.Key;
import dev.flashlabs.cratecrate.component.prize.Prize;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Config {

    public static final Map<String, Crate> CRATES = new HashMap<>();
    public static final Map<String, Reward> REWARDS = new HashMap<>();
    public static final Map<String, Prize> PRIZES = new HashMap<>();
    public static final Map<String, Key> KEYS = new HashMap<>();
    public static final Map<String, Effect> EFFECTS = new HashMap<>();

    private static final Path DIRECTORY = Sponge.configManager()
        .pluginConfig(CrateCrate.get().getContainer())
        .directory();

    public static void load() {
        try {
            Files.createDirectories(DIRECTORY.resolve("config"));
//            var main = load("cratecrate.conf", true);

            for (var path : listNodes("config/keys")) {
                var id = path.first();
                var node = path.second();
                Key key = resolveKeyType(node).deserializeComponent(id, node);
                KEYS.put(id, key);
            }

            for (var path : listNodes("config/prizes")) {
                var id = path.first();
                var node = path.second();
                Prize prize = resolvePrizeType(node).deserializeComponent(id, node);
                PRIZES.put(id, prize);
            }

            for (var path : listNodes("config/rewards")) {
                var id = path.first();
                var node = path.second();
                Reward reward = resolveRewardType(node).deserializeComponent(id, node);
                REWARDS.put(id, reward);
            }

            for (var path : listNodes("config/crates")) {
                var id = path.first();
                var node = path.second();
                Crate crate = resolveCrateType(node).deserializeComponent(id, node);
                CRATES.put(id, crate);
            }
            CrateCrate.get().logger().info("Successfully loaded the config.");
        } catch (IOException e) {
            CrateCrate.get().logger().error("Error loading the config: ", e);
        }
    }

    private static List<Tuple<String, ConfigurationNode>> listNodes(String sub) {
        try {
            var path = DIRECTORY.resolve(sub);

            Files.createDirectories(path);

            return Files.list(path).map(s -> {
                var node = load(s.toString());

                if(node == null) return null;

                return Tuple.of(s.getFileName().toString().split("\\.")[0], node);
            }).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static ConfigurationNode load(String name) {
        Path path = DIRECTORY.resolve(name);

        try {
            return GsonConfigurationLoader.builder().path(path).build().load();
        } catch (ConfigurateException e) {
            CrateCrate.get().logger().error("Failed to load: " + name);
            return null;
        }
    }

    public static Type<? extends Crate> resolveCrateType(ConfigurationNode node) throws SerializationException {
        return Config.resolveType(node, Crate.class, Crate.TYPES, CRATES);
    }

    public static Type<? extends Reward> resolveRewardType(ConfigurationNode node) throws SerializationException {
        return Config.resolveType(node, Reward.class, Reward.TYPES, REWARDS);
    }

    public static Type<? extends Prize> resolvePrizeType(ConfigurationNode node) throws SerializationException {
        return Config.resolveType(node, Prize.class, Prize.TYPES, PRIZES);
    }

    public static Type<? extends Key> resolveKeyType(ConfigurationNode node) throws SerializationException {
        return Config.resolveType(node, Key.class, Key.TYPES, KEYS);
    }

    public static Type<? extends Effect> resolveEffectType(ConfigurationNode node) throws SerializationException {
        return Config.resolveType(node, Effect.class, Effect.TYPES, EFFECTS);
    }

    private static <T extends Component<?>> Type<? extends T> resolveType(
        ConfigurationNode node,
        Class<T> component,
        Map<String, Type<? extends T>> types,
        Map<String, T> registry
    ) throws SerializationException {
        var identifier = Optional.ofNullable(node.getString()).orElse("");
        if (registry.containsKey(identifier)) {

            var typeType = types.get(registry.get(identifier).getClass().getName());

            if(typeType == null) {
                throw  new SerializationException("Type: " + identifier + " is null!");
            }

            return typeType;
        }
        if (node.hasChild("type")) {
            var type = node.node("type").getString();
            if (!types.containsKey(type)) {
                throw new SerializationException(node.node("type"), component, "Unknown type " + type + ".");
            }
            var typeType = types.get(type);

            if(typeType == null) {
                throw  new SerializationException("Type: " + type + " is null!");
            }

            return typeType;
        } else {
            for (var type : types.values()) {
                if(type.matches(node)) {
                    System.out.println("Testing this crap: " + type);
                    return type;
                }
            }
        }

        throw new SerializationException(node, component, "Unable to identify type.");
    }

}
