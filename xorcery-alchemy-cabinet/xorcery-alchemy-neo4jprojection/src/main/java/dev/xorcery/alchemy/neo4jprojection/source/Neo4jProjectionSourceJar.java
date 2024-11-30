package dev.xorcery.alchemy.neo4jprojection.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4jprojections.Neo4jProjectionUpdates;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

@Service(name = "projection")
public class Neo4jProjectionSourceJar
        implements SourceJar
{
    private final Configuration configuration;
    private final Logger logger;
    private final Neo4jProjectionUpdates neo4jProjectionUpdates;

    @Inject
    public Neo4jProjectionSourceJar(Configuration configuration, Logger logger, Neo4jProjectionUpdates neo4jProjectionUpdates) {
        this.configuration = configuration;
        this.logger = logger;
        this.neo4jProjectionUpdates = neo4jProjectionUpdates;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        return Flux.from(neo4jProjectionUpdates).map(this::convert);
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
