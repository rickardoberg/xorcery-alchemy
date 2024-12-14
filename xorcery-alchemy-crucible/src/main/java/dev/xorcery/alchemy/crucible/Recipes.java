package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class Recipes {

    private final CrucibleConfiguration crucibleConfiguration;

    @Inject
    public Recipes(Configuration configuration) {
        crucibleConfiguration = CrucibleConfiguration.get(configuration);
    }

    public List<RecipeConfiguration> getRecipes() {
        return crucibleConfiguration.getRecipes();
    }

    public Optional<RecipeConfiguration> getRecipeByName(String name)
    {
        for (RecipeConfiguration recipe : getRecipes()) {
            if (recipe.getName().map(n -> Objects.equals(n, name)).orElse(false))
            {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }
}
