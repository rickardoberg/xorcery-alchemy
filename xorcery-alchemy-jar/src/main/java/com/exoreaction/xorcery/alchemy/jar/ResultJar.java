package com.exoreaction.xorcery.alchemy.jar;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

public interface ResultJar
    extends Jar
{
    BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newResult(Configuration configuration, RecipeConfiguration recipeConfiguration);
}
