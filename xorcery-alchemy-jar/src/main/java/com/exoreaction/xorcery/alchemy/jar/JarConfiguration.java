package com.exoreaction.xorcery.alchemy.jar;

import com.exoreaction.xorcery.collections.Element;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;

import java.util.Map;
import java.util.Optional;

public record JarConfiguration(Configuration configuration)
    implements Element
{
    @Override
    public <T> Optional<T> get(String s) {
        return configuration.get(s);
    }

    public String getName() {
        return configuration.getString("name").orElseGet(this::getJar);
    }

    public boolean isEnabled(){
        return configuration.getBoolean("enabled").orElse(true);
    }

    public String getJar() {
        return configuration.getString("jar").orElseThrow(Configuration.missing("jar"));
    }

    public Map<String,Object> getContext()
    {
        return JsonElement.toMap(configuration.getConfiguration("context").json());
    }
}
