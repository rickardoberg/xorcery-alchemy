package dev.xorcery.alchemy.common.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

@Service(name = "flatten", metadata = "enabled=jars.enabled")
public class FlattenTransmuteJar
        implements TransmuteJar {

    @Inject
    public FlattenTransmuteJar() {
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {

        return (flux, context) ->
                flux.flatMapIterable(new FlattenTransmute(configuration, recipeConfiguration));
    }
}
