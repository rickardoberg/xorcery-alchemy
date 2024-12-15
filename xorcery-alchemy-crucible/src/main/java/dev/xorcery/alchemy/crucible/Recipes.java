package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Optional;

@Service
public class Recipes {

    private final RecipesConfiguration recipesConfiguration;

    @Inject
    public Recipes(Configuration configuration) {
        recipesConfiguration = RecipesConfiguration.get(configuration);
    }

    public List<TransmutationConfiguration> getRecipes() {
        return recipesConfiguration.getRecipes();
    }

    public Optional<TransmutationConfiguration> getRecipeByName(String name)
    {
        return recipesConfiguration.getRecipe(name);
    }
}
