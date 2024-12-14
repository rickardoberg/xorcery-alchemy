package dev.xorcery.alchemy.jar;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import reactor.core.publisher.Flux;

public class Transmutation {
    private final TransmutationConfiguration transmutationConfiguration;
    private final Flux<MetadataJsonNode<JsonNode>> flux;

    public Transmutation(TransmutationConfiguration transmutationConfiguration, Flux<MetadataJsonNode<JsonNode>> flux) {
        this.transmutationConfiguration = transmutationConfiguration;
        this.flux = flux;
    }

    public TransmutationConfiguration getTransmutationConfiguration() {
        return transmutationConfiguration;
    }

    public Flux<MetadataJsonNode<JsonNode>> getFlux() {
        return flux;
    }
}
