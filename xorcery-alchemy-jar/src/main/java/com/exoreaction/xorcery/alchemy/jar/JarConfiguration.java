package com.exoreaction.xorcery.alchemy.jar;

import com.exoreaction.xorcery.configuration.Configuration;

public record JarConfiguration(Configuration configuration) {
    public String getName() {
        return configuration.getString("name").orElseGet(this::getJar);
    }

    public boolean isEnabled(){
        return configuration.getBoolean("enabled").orElse(true);
    }

    public String getJar() {
        return configuration.getString("jar").orElseThrow(Configuration.missing("jar"));
    }
}
