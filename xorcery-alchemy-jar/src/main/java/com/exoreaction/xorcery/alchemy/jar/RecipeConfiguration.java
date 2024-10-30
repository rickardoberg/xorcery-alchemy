package com.exoreaction.xorcery.alchemy.jar;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record RecipeConfiguration(Configuration configuration) {
    public String getName() {
        return configuration.getString("name").orElseThrow(Configuration.missing("name"));
    }

    public boolean isEnabled(){
        return configuration.getBoolean("enabled").orElse(true);
    }

    public Map<String, Object> getContext()
    {
        return JsonElement.toMap(configuration.getConfiguration("context").json());
    }

    public JarConfiguration getSource() {
        return new JarConfiguration(configuration.getConfiguration("source"));
    }

    public List<JarConfiguration> getTransmutes() {
        return configuration.getObjectListAs("transmutes", json -> new JarConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }

    public JarConfiguration getResult() {
        return new JarConfiguration(configuration.getConfiguration("result"));
    }
}
