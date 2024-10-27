package com.exoreaction.xorcery.alchemy.plugin;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record TransmutationConfiguration(Configuration configuration) {
    public String getName() {
        return configuration.getString("name").orElseThrow(Configuration.missing("name"));
    }

    public boolean isEnabled(){
        return configuration.getBoolean("enabled").orElse(true);
    }

    public PluginConfiguration getInput() {
        return new PluginConfiguration(configuration.getConfiguration("input"));
    }

    public List<PluginConfiguration> getTransmutes() {
        return configuration.getObjectListAs("transmutes", json -> new PluginConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }

    public PluginConfiguration getOutput() {
        return new PluginConfiguration(configuration.getConfiguration("output"));
    }
}
