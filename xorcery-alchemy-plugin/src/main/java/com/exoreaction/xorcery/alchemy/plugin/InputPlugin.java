package com.exoreaction.xorcery.alchemy.plugin;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;

public interface InputPlugin
    extends Plugin
{
    Flux<MetadataJsonNode<JsonNode>> newInstance(Configuration configuration, TransmutationConfiguration transmutationConfiguration);
}
