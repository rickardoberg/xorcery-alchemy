package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CrucibleConfiguration(Configuration configuration) {
    public static CrucibleConfiguration get(Configuration configuration) {
        return new CrucibleConfiguration(configuration.getConfiguration("crucible"));
    }

    public boolean isCloseWhenDone() {
        return configuration.getBoolean("closeWhenDone").orElse(true);
    }

    public List<RecipeConfiguration> getRecipes() {
        return configuration.getObjectListAs("recipes", json -> new RecipeConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }

    public Optional<RecipeConfiguration> getRecipe(String name) {
        return getRecipes().stream()
                .filter(recipe -> Objects.equals(recipe.getName().orElse(null), name))
                .findFirst();
    }

    public boolean isRecipeReference(String name) {
        return getRecipes().stream().anyMatch(recipe -> Objects.equals(recipe.getName().orElse(null), name));
    }

    public List<TransmutationConfiguration> getTransmutations() {
        return configuration.getObjectListAs("transmutations", json -> new TransmutationConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }
}
