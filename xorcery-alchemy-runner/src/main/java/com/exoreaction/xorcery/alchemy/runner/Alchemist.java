package com.exoreaction.xorcery.alchemy.runner;


import com.exoreaction.xorcery.alchemy.plugin.*;
import com.exoreaction.xorcery.concurrent.CompletableFutures;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service(name = "alchemist")
@RunLevel(20)
public class Alchemist {

    private final Configuration configuration;
    private final Logger logger;
    private final Plugins plugins;
    private final CompletableFuture<Void> result;

    @Inject
    public Alchemist(Configuration configuration, Plugins plugins, Logger logger) {
        this.configuration = configuration;
        this.plugins = plugins;
        this.logger = logger;
        this.result = new CompletableFuture<>();
        logger.info("Starting alchemist");

        AlchemistConfiguration alchemistConfiguration = AlchemistConfiguration.get(configuration);
        alchemistConfiguration.getTransmutations().forEach(transmutationConfiguration ->
                addTransmutation(transmutationConfiguration).whenComplete(CompletableFutures.transfer(result)));
    }

    public CompletableFuture<Void> addTransmutation(TransmutationConfiguration transmutation) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        logger.info("Starting transmutation: " + transmutation.getName());
        Flux<MetadataJsonNode<JsonNode>> inputFlux = getInput(transmutation);
        Flux<MetadataJsonNode<JsonNode>> transmutedFlux = applyTransmutes(transmutation, inputFlux);
        Flux<MetadataJsonNode<JsonNode>> outputFlux = applyOutput(transmutation, transmutedFlux);
        outputFlux.retryWhen(getRetry(transmutation));
        outputFlux.subscribe(item -> {
        }, result::completeExceptionally, () -> result.complete(null));
        return result;
    }

    public CompletableFuture<Void> getResult() {
        return result;
    }

    private Flux<MetadataJsonNode<JsonNode>> getInput(TransmutationConfiguration transmutation) {
        PluginConfiguration inputConfiguration = transmutation.getInput();
        InputPlugin inputPlugin = plugins.getInputPlugin(inputConfiguration.getPlugin()).orElseThrow(() -> new IllegalArgumentException("No input plugin named:" + inputConfiguration.getPlugin()));
        Flux<MetadataJsonNode<JsonNode>> inputFlux = inputPlugin.newInstance(inputConfiguration.configuration(), transmutation);
        return inputFlux;
    }

    private Flux<MetadataJsonNode<JsonNode>> applyTransmutes(TransmutationConfiguration transmutation, Flux<MetadataJsonNode<JsonNode>> transmutedFlux) {
        for (PluginConfiguration transmuteConfiguration : transmutation.getTransmutes()) {
            TransmutePlugin outputPlugin = plugins.getTransmutePlugin(transmuteConfiguration.getPlugin()).orElseThrow(() -> new IllegalArgumentException("No transmute plugin named:" + transmuteConfiguration.getPlugin()));
            transmutedFlux = transmutedFlux.transformDeferredContextual(outputPlugin.newInstance(transmuteConfiguration.configuration(), transmutation));
        }
        return transmutedFlux;
    }

    private Flux<MetadataJsonNode<JsonNode>> applyOutput(TransmutationConfiguration transmutation, Flux<MetadataJsonNode<JsonNode>> transmutedFlux) {
        PluginConfiguration outputConfiguration = transmutation.getOutput();
        OutputPlugin outputPlugin = plugins.getOutputPlugin(outputConfiguration.getPlugin()).orElseThrow(() -> new IllegalArgumentException("No output plugin named:" + outputConfiguration.getPlugin()));
        return transmutedFlux.transformDeferredContextual(outputPlugin.newInstance(outputConfiguration.configuration(), transmutation));
    }

    private Retry getRetry(TransmutationConfiguration transmutationConfiguration) {
        return Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10));
    }
}
