package com.exoreaction.xorcery.alchemy.plugin;

import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;

@Service
public class Plugins {

    private final IterableProvider<Plugin> plugins;

    @Inject
    public Plugins(IterableProvider<Plugin> plugins, Logger logger) {
        this.plugins = plugins;

        for (ServiceHandle<Plugin> plugin : plugins.handleIterator()) {
            logger.info("Plugin:" + plugin.getActiveDescriptor().getName().substring("plugin.".length()));
        }
    }

    public Optional<InputPlugin> getInputPlugin(String pluginName) {

        for (ServiceHandle<Plugin> plugin : plugins.handleIterator()) {
            if (plugin.getActiveDescriptor().getName().endsWith(pluginName) &&
                    InputPlugin.class.isAssignableFrom(plugin.getActiveDescriptor().getImplementationClass())) {
                return Optional.of((InputPlugin)plugin.getService());
            }
        }
        return Optional.empty();
    }

    public Optional<TransmutePlugin> getTransmutePlugin(String pluginName) {

        for (ServiceHandle<Plugin> plugin : plugins.handleIterator()) {
            if (plugin.getActiveDescriptor().getName().endsWith(pluginName) &&
                    TransmutePlugin.class.isAssignableFrom(plugin.getActiveDescriptor().getImplementationClass())) {
                return Optional.of((TransmutePlugin) plugin.getService());
            }
        }
        return Optional.empty();
    }

    public Optional<OutputPlugin> getOutputPlugin(String pluginName) {

        for (ServiceHandle<Plugin> plugin : plugins.handleIterator()) {
            if (plugin.getActiveDescriptor().getName().endsWith(pluginName) &&
                    OutputPlugin.class.isAssignableFrom(plugin.getActiveDescriptor().getImplementationClass())) {
                return Optional.of((OutputPlugin) plugin.getService());
            }
        }
        return Optional.empty();
    }
}
