package dev.xorcery.alchemy.jar;

import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record RecipeConfiguration(Configuration configuration) {
    public Optional<String> getName() {
        return configuration.getString("name");
    }

    public boolean isEnabled(){
        return configuration.getBoolean("enabled").orElse(true);
    }

    public JarConfiguration getSource() {
        return new JarConfiguration(configuration.getConfiguration("source"));
    }

    public List<JarConfiguration> getTransmutes() {
        return configuration.getObjectListAs("transmutes", json -> new JarConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }
}
