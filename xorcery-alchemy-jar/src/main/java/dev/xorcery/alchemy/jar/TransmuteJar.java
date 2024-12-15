package dev.xorcery.alchemy.jar;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

public interface TransmuteJar
    extends Jar
{
    BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration);
}
