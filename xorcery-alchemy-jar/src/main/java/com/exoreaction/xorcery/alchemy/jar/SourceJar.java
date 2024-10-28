package com.exoreaction.xorcery.alchemy.jar;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;

public interface SourceJar
    extends Jar
{
    Flux<MetadataJsonNode<JsonNode>> newSource(Configuration configuration, RecipeConfiguration recipeConfiguration);
}
