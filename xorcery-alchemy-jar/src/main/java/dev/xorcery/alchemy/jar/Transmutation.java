package dev.xorcery.alchemy.jar;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import reactor.core.publisher.Flux;

public class Transmutation {
    private final RecipeConfiguration recipeConfiguration;
    private final Flux<MetadataJsonNode<JsonNode>> flux;

    public Transmutation(RecipeConfiguration recipeConfiguration, Flux<MetadataJsonNode<JsonNode>> flux) {
        this.recipeConfiguration = recipeConfiguration;
        this.flux = flux;
    }

    public RecipeConfiguration getRecipeConfiguration() {
        return recipeConfiguration;
    }

    public Flux<MetadataJsonNode<JsonNode>> getFlux() {
        return flux;
    }
}
