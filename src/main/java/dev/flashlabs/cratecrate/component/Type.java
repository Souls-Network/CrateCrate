package dev.flashlabs.cratecrate.component;

import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.plugin.PluginContainer;

import java.util.List;

public abstract class Type<T extends Component<?>> {

    private final String name;
    private final PluginContainer container;

    protected Type(String name, PluginContainer container) {
        this.name = name;
        this.container = container;
    }

    public final String name() {
        return name;
    }

    public final PluginContainer container() {
        return container;
    }

    public T deserializeComponent(Tuple<String, ConfigurationNode> tuple) throws SerializationException {
        return deserializeComponent(tuple.first(), tuple.second());
    }

    public abstract T deserializeComponent(String id, ConfigurationNode node) throws SerializationException;

    public abstract ValueHolder<T, ?> deserializeReference(ConfigurationNode node) throws SerializationException;

}
