package dev.flashlabs.cratecrate;

import com.google.inject.Inject;
import dev.flashlabs.cratecrate.command.Base;
import dev.flashlabs.cratecrate.component.Crate;
import dev.flashlabs.cratecrate.component.Reward;
import dev.flashlabs.cratecrate.component.effect.*;
import dev.flashlabs.cratecrate.component.key.Key;
import dev.flashlabs.cratecrate.component.key.StandardKey;
import dev.flashlabs.cratecrate.component.prize.CommandPrize;
import dev.flashlabs.cratecrate.component.prize.ItemPrize;
import dev.flashlabs.cratecrate.component.prize.MoneyPrize;
import dev.flashlabs.cratecrate.component.prize.Prize;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Listeners;
import dev.flashlabs.cratecrate.internal.Storage;
import dev.flashlabs.flashlibs.plugin.PluginInstance;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.LoadedGameEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.MapFactory;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.Map;
import java.util.function.Consumer;

@Plugin("cratecrate")
public final class CrateCrate extends PluginInstance {

    private static CrateCrate instance;

    @Inject
    private CrateCrate(PluginContainer container) {
        super(container);
        instance = this;
    }

    @Listener
    public void onConstruct(ConstructPluginEvent event) {
        Crate.TYPES.put(Crate.TYPE.name(), Crate.TYPE);
        Reward.TYPES.put(Reward.TYPE.name(), Reward.TYPE);
        Prize.TYPES.put(CommandPrize.TYPE.name(), CommandPrize.TYPE);
        Prize.TYPES.put(ItemPrize.TYPE.name(), ItemPrize.TYPE);
        Prize.TYPES.put(MoneyPrize.TYPE.name(), MoneyPrize.TYPE);
        Key.TYPES.put(StandardKey.TYPE.name(), StandardKey.TYPE);
        Effect.TYPES.put(FireworkEffect.TYPE.name(), FireworkEffect.TYPE);
        Effect.TYPES.put(ParticleEffect.TYPE.name(), ParticleEffect.TYPE);
        Effect.TYPES.put(PotionEffect.TYPE.name(), PotionEffect.TYPE);
        Effect.TYPES.put(SoundEffect.TYPE.name(), SoundEffect.TYPE);
        Sponge.eventManager().registerListeners(container, new Listeners());
    }

    @Listener
    public void onLoaded(LoadedGameEvent event) {
        Config.load();
        Storage.load();
    }

    @Listener
    public void onRefresh(RefreshGameEvent event) {
        Config.load();
        Storage.load();
    }

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Parameterized> event) {
        event.register(getContainer(), Base.COMMAND, "cratecrate", "crate");
    }

    public static CrateCrate get() {
        return instance;
    }
}
