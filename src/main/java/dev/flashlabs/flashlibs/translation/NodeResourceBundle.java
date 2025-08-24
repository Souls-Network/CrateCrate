package dev.flashlabs.flashlibs.translation;

import org.spongepowered.configurate.ConfigurationNode;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Represents a {@link ResourceBundle} loaded from an {@link ConfigurationNode}.
 * All values are registered under the path to the node, consisting of all keys
 * joined with {@code '.'}.
 */
final class NodeResourceBundle extends ResourceBundle {

    private final Map<String, Object> map = new HashMap<>();

    NodeResourceBundle(ConfigurationNode node) {
        load(node);
    }

    private void load(ConfigurationNode node) {
        if (node.isList()) {
            node.childrenList().forEach(this::load);
        } else if (node.isMap()) {
            node.childrenMap().values().forEach(this::load);
        } else {
            map.put(Arrays.stream(node.path().array())
                .map(Object::toString)
                .collect(Collectors.joining(".")), node.raw());
        }
    }

    @Override
    protected Object handleGetObject(String key) {
        return map.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return new Enumeration<>() {

            private final Iterator<String> keys = map.keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return keys.hasNext();
            }

            @Override
            public String nextElement() {
                return keys.next();
            }

        };
    }
}
