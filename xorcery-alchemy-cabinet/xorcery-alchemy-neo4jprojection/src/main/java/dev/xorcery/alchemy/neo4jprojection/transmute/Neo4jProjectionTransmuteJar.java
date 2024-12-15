package dev.xorcery.alchemy.neo4jprojection.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4jprojections.api.Neo4jProjections;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static dev.xorcery.neo4jprojections.api.ProjectionStreamContext.projectionId;
import static dev.xorcery.reactivestreams.api.ContextViewElement.missing;

@Service(name = "projection", metadata = "enabled=jars.enabled")
public class Neo4jProjectionTransmuteJar
        implements TransmuteJar {
    private final Neo4jProjections neo4jProjections;

    @Inject
    public Neo4jProjectionTransmuteJar(Neo4jProjections neo4jProjections) {
        this.neo4jProjections = neo4jProjections;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {
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
                .contextWrite(Context.of(projectionId, jarConfiguration.getString(projectionId).orElseThrow(missing(projectionId))))
                .map(metadataEvents -> {
                    return new MetadataJsonNode<>(metadataEvents.metadata(), JsonNodeFactory.instance.nullNode());
                });
    }
}
