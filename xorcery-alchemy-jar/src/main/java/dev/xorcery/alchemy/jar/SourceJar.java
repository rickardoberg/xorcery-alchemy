package dev.xorcery.alchemy.jar;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import reactor.core.publisher.Flux;

public interface SourceJar
    extends Jar
{
    Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration);
}
