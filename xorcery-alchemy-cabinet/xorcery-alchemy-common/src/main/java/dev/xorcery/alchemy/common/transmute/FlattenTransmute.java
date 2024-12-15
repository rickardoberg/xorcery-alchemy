package dev.xorcery.alchemy.common.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FlattenTransmute implements Function<MetadataJsonNode<JsonNode>, Iterable<? extends MetadataJsonNode<JsonNode>>> {
    private final Predicate<String> includeKey;
    private final Function<String, String> nameMapper;
    private final String prefix;


    public FlattenTransmute(JarConfiguration configuration, TransmutationConfiguration transmutationConfiguration) {
        includeKey = key -> true;
        nameMapper = n -> n;
        prefix = "";
    }

    @Override
    public Iterable<? extends MetadataJsonNode<JsonNode>> apply(MetadataJsonNode<JsonNode> item) {

        if (item.data() instanceof ObjectNode objectNode)
        {
            return flatten(objectNode, prefix.isEmpty() ? "" : prefix + ".").map(data -> new MetadataJsonNode<JsonNode>(item.metadata(), data)).toList();
        } else
        {
            return Collections.singletonList(item);
        }
    }

    private Stream<ObjectNode> flatten(ObjectNode row, String namePrefix) {
        ObjectNode values = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, JsonNode> property : row.properties()) {
            String mappedName = nameMapper.apply(namePrefix + property.getKey());
            if (includeKey.test(mappedName)) {
                JsonNode propertyValue = property.getValue();
                if (!(propertyValue instanceof ObjectNode || propertyValue instanceof ArrayNode)) {
                    values.set(mappedName, propertyValue);
                }
            }
        }
        Stream<ObjectNode> result = Stream.of(values);

        for (Map.Entry<String, JsonNode> property : row.properties()) {
            if (property.getValue() instanceof ObjectNode propertyObjectNode) {
                Stream<ObjectNode> objectResult = flatten(propertyObjectNode, namePrefix + property.getKey() + ".");
                result = result.mapMulti(combine(objectResult));
            } else if (property.getValue() instanceof ArrayNode list && !list.isEmpty()) {
                Stream<ObjectNode> arrayStream = Stream.empty();
                for (JsonNode item : list) {
                    if (item instanceof ObjectNode objectNode) {
                        Stream<ObjectNode> objectResult = flatten(objectNode, namePrefix + property.getKey() + ".");
                        arrayStream = Stream.concat(arrayStream, objectResult);
                    }
                }
                result = result.mapMulti(combine(arrayStream));
            }
        }
        return result;
    }

    private static BiConsumer<ObjectNode, Consumer<ObjectNode>> combine(Stream<ObjectNode> objectResult) {
        List<ObjectNode> listResult = objectResult.toList();
        return (r, c) ->
        {
            listResult.stream().map(m -> {
                ObjectNode combined = JsonNodeFactory.instance.objectNode().setAll(r);
                combined.setAll(m);
                return combined;
            }).forEach(c);
        };
    }
}
