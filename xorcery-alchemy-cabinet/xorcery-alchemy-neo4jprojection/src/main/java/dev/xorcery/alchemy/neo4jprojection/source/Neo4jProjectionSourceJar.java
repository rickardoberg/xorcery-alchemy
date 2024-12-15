package dev.xorcery.alchemy.neo4jprojection.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4jprojections.api.Neo4jProjections;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import static dev.xorcery.neo4jprojections.api.ProjectionStreamContext.projectionId;
import static dev.xorcery.reactivestreams.api.ContextViewElement.missing;

@Service(name = "projection", metadata = "enabled=jars.enabled")
public class Neo4jProjectionSourceJar
        implements SourceJar
{
    private final Neo4jProjections neo4jProjections;

    @Inject
    public Neo4jProjectionSourceJar(Neo4jProjections neo4jProjections) {
        this.neo4jProjections = neo4jProjections;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {
        return Flux.from(neo4jProjections.projectionUpdates())
                .contextWrite(Context.of(projectionId, jarConfiguration.getString(projectionId).orElseThrow(missing(projectionId))))
                .map(this::convert);
    }

    private MetadataJsonNode<JsonNode> convert(MetadataEvents metadataEvents) {
        ArrayNode eventArray = JsonNodeFactory.instance.arrayNode(metadataEvents.data().size());
        for (DomainEvent domainEvent : metadataEvents.data()) {
            if (domainEvent instanceof JsonDomainEvent jsonDomainEvent)
            {
                eventArray.add(jsonDomainEvent.event());
            }
        }
        return new MetadataJsonNode<>(metadataEvents.metadata(), eventArray);
    }
}
