package dev.xorcery.alchemy.domainevents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarException;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.Value;
import dev.xorcery.json.JsonElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import graphql.schema.*;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.function.BiFunction;

import static dev.xorcery.collections.Element.missing;

@Service(name = "graphqldomainevents", metadata = "enabled=jars.enabled")
public class GraphQLDomainEventTransmuteJar
        implements TransmuteJar {

    private final GraphQLSchema graphQLSchema;

    @Inject
    public GraphQLDomainEventTransmuteJar(GraphQLSchema graphQLSchema) {
        this.graphQLSchema = graphQLSchema;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        ObjectNode metadataConfig = jarConfiguration.configuration().getConfiguration("metadata").json();
        return (flux, context) -> flux.handle((item, sink) -> {
            try {
                String entityType = item.metadata().getString("entity").orElseThrow(missing("entity"));
                if (graphQLSchema.getType(entityType) instanceof GraphQLObjectType entityObjectType) {
                    JsonElement data = item::data;
                    JsonDomainEvent.StateBuilder eventStateBuilder = JsonDomainEvent.event("ImportedData")
                            .updated(entityType, data.getString("id").orElseThrow(missing("id")));
                    for (Map.Entry<String, JsonNode> property : data.object().properties()) {
                        if (property.getKey().equals("id"))
                            continue;
                        GraphQLFieldDefinition fieldDefinition = entityObjectType.getFieldDefinition(property.getKey());
                        if (fieldDefinition == null) {
                            sink.error(new IllegalArgumentException("No property " + property.getKey() + " on type " + entityType));
                            return;
                        }
                        GraphQLType fieldType = GraphQLHelpers.inner(fieldDefinition.getType());
                        if (fieldType instanceof GraphQLScalarType) {
                            eventStateBuilder.updatedAttribute(property.getKey(), property.getValue());
                        } else if (fieldType instanceof GraphQLObjectType graphQLObjectType) {
                            if (GraphQLHelpers.isEntity(graphQLObjectType)) {

                                RelationDirective.get(fieldDefinition).ifPresent(relationDirective -> {
                                    if (relationDirective.direction().equals("OUTGOING")) {
                                        if (GraphQLHelpers.isList(fieldDefinition.getType())) {
                                            eventStateBuilder.addedRelationship(relationDirective.name(), graphQLObjectType.getName(), property.getValue().asText());
                                        } else {
                                            eventStateBuilder.updatedRelationship(relationDirective.name(), graphQLObjectType.getName(), property.getValue().asText());
                                        }
                                    }
                                });
                            } else {
                                // Complex value attribute
                                if (property.getValue() instanceof ObjectNode valueJson) {
                                    eventStateBuilder.updatedAttribute(property.getKey(), Value.value().with(v -> valueJson.properties().forEach(e -> v.attribute(e.getKey(), e.getValue()))));
                                } else if (property.getValue() instanceof ArrayNode valuesJson) {
                                    eventStateBuilder.removedAttribute(property.getKey(), Value.value());
                                    int idx = 0;
                                    for (JsonNode jsonNode : valuesJson) {
                                        if (jsonNode instanceof ObjectNode valueJson)
                                            eventStateBuilder.addedAttribute(property.getKey(), idx++, Value.value().with(v -> valueJson.properties().forEach(e -> v.attribute(e.getKey(), e.getValue()))));
                                        else
                                            eventStateBuilder.addedAttribute(property.getKey(), idx++, jsonNode);
                                    }
                                }
                            }
                        }
                    }
                    JsonDomainEvent event = eventStateBuilder.build();

                    item.metadata().json().setAll(metadataConfig);
                    sink.next(new MetadataJsonNode<>(item.metadata(), JsonNodeFactory.instance.arrayNode(1).add(event.event())));
                } else {
                    sink.error(new IllegalArgumentException(String.format("Entity type %s is not an object in the GraphQL schema", entityType)));
                }
            } catch (Throwable error) {
                sink.error(new JarException(jarConfiguration, recipeConfiguration, String.format("Could not handle item:%s(%s)", item.data(), item.metadata()), error));
            }
        });
    }
}
