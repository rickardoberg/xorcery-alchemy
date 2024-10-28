package com.exoreaction.xorcery.alchemy.crucible;

import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record CrucibleConfiguration(Configuration configuration) {
    public static CrucibleConfiguration get(Configuration configuration) {
        return new CrucibleConfiguration(configuration.getConfiguration("crucible"));
    }

    public boolean isCloseWhenDone()
    {
        return configuration.getBoolean("closeWhenDone").orElse(true);
    }

    public List<RecipeConfiguration> getRecipes() {
        return configuration.getObjectListAs("recipes", json -> new RecipeConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }
}
