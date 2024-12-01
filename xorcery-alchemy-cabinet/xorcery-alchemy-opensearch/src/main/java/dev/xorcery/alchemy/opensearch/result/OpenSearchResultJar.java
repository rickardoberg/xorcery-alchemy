package dev.xorcery.alchemy.opensearch.result;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.ResultJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.opensearch.OpenSearchService;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.util.UUIDs;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

@Service(name="opensearch")
public class OpenSearchResultJar
    implements ResultJar
{
    private final OpenSearchService openSearchService;

    @Inject
    public OpenSearchResultJar(Configuration configuration, Logger logger, OpenSearchService openSearchService) {
        this.openSearchService = openSearchService;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newResult(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        return (flux, context)->{
            return flux.transformDeferredContextual(openSearchService.documentUpdates(item -> UUIDs.newId()));
        };
    }
}
