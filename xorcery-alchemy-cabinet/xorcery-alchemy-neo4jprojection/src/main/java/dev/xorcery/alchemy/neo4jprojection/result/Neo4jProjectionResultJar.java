package dev.xorcery.alchemy.neo4jprojection.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.ResultJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4jprojections.api.Neo4jProjections;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Service(name = "projection")
public class Neo4jProjectionResultJar
        implements ResultJar {
    private final Configuration configuration;
    private final Logger logger;
    private final Neo4jProjections neo4jProjections;

    @Inject
    public Neo4jProjectionResultJar(Configuration configuration, Logger logger, Neo4jProjections neo4jProjections) {
        this.configuration = configuration;
        this.logger = logger;
        this.neo4jProjections = neo4jProjections;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newResult(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        return (flux, context) -> flux.<MetadataEvents>handle((metadatajson, sink) -> {
                    if (metadatajson.data() instanceof ArrayNode arrayNode) {
                        List<DomainEvent> events = new ArrayList<>(arrayNode.size());
                        arrayNode.forEach(item ->
                        {
                            if (item instanceof ObjectNode objectNode) {
                                events.add(new JsonDomainEvent(objectNode));
                            }
                        });
                        sink.next(new MetadataEvents(metadatajson.metadata(), events));
                    }
                }).transformDeferredContextual(neo4jProjections.projection())
                .map(metadataEvents -> {
                    return new MetadataJsonNode<>(metadataEvents.metadata(), JsonNodeFactory.instance.nullNode());
                });
    }
}
