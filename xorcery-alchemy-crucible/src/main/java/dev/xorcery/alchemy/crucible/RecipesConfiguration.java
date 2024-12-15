package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RecipesConfiguration(List<TransmutationConfiguration> recipes) {

    public static RecipesConfiguration get(Configuration configuration){
        return new RecipesConfiguration(configuration.getObjectListAs("recipes",
                json -> new TransmutationConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList()));
    }

    public List<TransmutationConfiguration> getRecipes() {
        return recipes;
    }

    public Optional<TransmutationConfiguration> getRecipe(String name) {
        return recipes.stream()
                .filter(recipe -> Objects.equals(recipe.getName().orElse(null), name))
                .findFirst();
    }
}
