package dev.xorcery.alchemy.common.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Service(name = "rename")
public class RenameTransmuteJar
        implements TransmuteJar {
    @Inject
    public RenameTransmuteJar() {
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        Configuration metadataRenameConfiguration = configuration.configuration().getConfiguration("metadata");
        Map<String, String> renamedMetadata = new HashMap<>();
        for (Map.Entry<String, JsonNode> renamedField : metadataRenameConfiguration.json().properties()) {
            renamedMetadata.put(renamedField.getKey(), renamedField.getValue().asText());
        }
        Configuration dataRenameConfiguration = configuration.configuration().getConfiguration("data");
        Map<String, String> renamedData = new HashMap<>();
        for (Map.Entry<String, JsonNode> renamedField : dataRenameConfiguration.json().properties()) {
            renamedData.put(renamedField.getKey(), renamedField.getValue().asText());
        }
        return (flux, context) ->
                flux.map(item ->
                {
                    ObjectNode metadata = item.metadata().json();
                    if (!renamedMetadata.isEmpty()) {
                        ObjectNode renamedMetadataJson = JsonNodeFactory.instance.objectNode();
                        renamedMetadata.forEach((to, from) ->
                        {
                            renamedMetadataJson.set(to, item.data().get(from));
                        });
                        metadata = renamedMetadataJson;
                    }

                    JsonNode data = item.data();
                    if (!renamedData.isEmpty() && data instanceof ObjectNode) {
                        ObjectNode renamedDataJson = JsonNodeFactory.instance.objectNode();
                        renamedData.forEach((to, from) ->
                        {
                            renamedDataJson.set(to, item.data().get(from));
                        });
                        data = renamedDataJson;
                    }

                    return new MetadataJsonNode<>(new Metadata(metadata), data);
                });
    }
}
