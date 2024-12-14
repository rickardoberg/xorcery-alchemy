package dev.xorcery.alchemy.jar;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;

import java.util.Optional;

public record TransmutationConfiguration(Configuration configuration) {
    public Optional<String> getName() {
        return configuration.getString("name");
    }

    public boolean isEnabled(){
        return configuration.getBoolean("enabled").orElse(true);
    }

    public RecipeConfiguration getRecipe()
    {
        return new RecipeConfiguration(configuration.getConfiguration("recipe"));
    }

    public ObjectNode getContext()
    {
        return configuration.getConfiguration("context").object();
    }
}
