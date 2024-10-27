package com.exoreaction.xorcery.alchemy.plugin;

import com.exoreaction.xorcery.configuration.Configuration;

public record PluginConfiguration(Configuration configuration) {
    public String getName() {
        return configuration.getString("name").orElseGet(this::getPlugin);
    }

    public boolean isEnabled(){
        return configuration.getBoolean("enabled").orElse(true);
    }

    public String getPlugin() {
        return configuration.getString("plugin").orElseThrow(Configuration.missing("plugin"));
    }
}
