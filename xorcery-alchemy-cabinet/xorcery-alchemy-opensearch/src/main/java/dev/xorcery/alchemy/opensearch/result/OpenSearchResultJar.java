package dev.xorcery.alchemy.opensearch.result;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarContext;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.ResultJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.lang.Exceptions;
import dev.xorcery.opensearch.OpenSearchService;
import dev.xorcery.opensearch.client.search.Document;
import dev.xorcery.opensearch.client.search.SearchRequest;
import dev.xorcery.opensearch.client.search.SearchResponse;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.util.UUIDs;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
        return (flux, context)-> {
            return flux.contextWrite(ctx ->
            {
                // Find last metadata.streamPosition for this sourceUrl under the given alias
                ContextViewElement cve = new ContextViewElement(ctx);
                String sourceUrl = cve.getString(JarContext.sourceUrl).orElse(null);
                if (sourceUrl == null)
                    return ctx;

                String alias = cve.getString("alias").orElse(null);
                if (alias == null)
                    return ctx;

                try {
                    SearchRequest request = SearchRequest.builder()
                            .query(QueryDSL.match_phrase("metadata.sourceUrl", sourceUrl))
                            .build();
                    SearchResponse response = openSearchService.getClient().search().search(alias, request, Map.of("size", "1", "sort", "metadata.streamPosition:desc")).orTimeout(10, TimeUnit.SECONDS).join();

                    List<Document> documents = response.hits().documents();
                    if (!documents.isEmpty())
                    {
                        long streamPosition = documents.get(0).json().path("_source").path("metadata").path("streamPosition").longValue();
                        return ctx.put(JarContext.streamPosition.name(), streamPosition);
                    }
                    return ctx;
                } catch (Throwable e) {
                    if (Exceptions.unwrap(e) instanceof NotFoundException)
                    {
                        // Ok!
                        return ctx;
                    } else {
                        throw (RuntimeException)e;
                    }
                }
            }).transformDeferredContextual(openSearchService.documentUpdates(item -> UUIDs.newId()));
        };
    }
}
