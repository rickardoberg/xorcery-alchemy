package dev.xorcery.alchemy.jar;

import dev.xorcery.collections.Element;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;

import java.util.Map;
import java.util.Optional;

public record JarConfiguration(Configuration configuration)
    implements Element
{
    @Override
    public <T> Optional<T> get(String s) {
        return configuration.get(s);
    }

    public Optional<String> getName() {
        return configuration.getString("name");
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