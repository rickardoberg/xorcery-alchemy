package dev.xorcery.alchemy.opensearch.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarContext;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.lang.Exceptions;
import dev.xorcery.opensearch.OpenSearchService;
import dev.xorcery.opensearch.client.search.Document;
import dev.xorcery.opensearch.client.search.SearchRequest;
import dev.xorcery.opensearch.client.search.SearchResponse;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.util.UUIDs;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service(name="opensearch", metadata = "enabled=jars.enabled")
public class OpenSearchTransmuteJar
    implements TransmuteJar
{
    private final OpenSearchService openSearchService;

    @Inject
    public OpenSearchTransmuteJar(OpenSearchService openSearchService) {
        this.openSearchService = openSearchService;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {
        return (flux, context)-> flux.contextWrite(ctx ->
        {
            // Find last metadata.streamPosition for this sourceUrl under the given alias
            String sourceUrl = jarConfiguration.getString(JarContext.sourceUrl).orElse(null);
            if (sourceUrl == null)
                return ctx;

            String alias = jarConfiguration.getString("alias").orElse(null);
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
        }).transformDeferredContextual(openSearchService.documentUpdates(item -> UUIDs.newId(), Function.identity()));
    }
}
