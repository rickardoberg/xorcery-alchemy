package com.exoreaction.xorcery.alchemy.crucible;

import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record CrucibleConfiguration(Configuration configuration) {
    public static CrucibleConfiguration get(Configuration configuration) {
        return new CrucibleConfiguration(configuration.getConfiguration("crucible"));
    }

    public List<RecipeConfiguration> getTransmutations() {
        return configuration.getObjectListAs("transmutations", json -> new RecipeConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }
}
