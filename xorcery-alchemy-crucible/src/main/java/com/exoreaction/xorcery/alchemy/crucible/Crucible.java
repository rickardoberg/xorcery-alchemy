package com.exoreaction.xorcery.alchemy.crucible;


import com.exoreaction.xorcery.alchemy.jar.*;
import com.exoreaction.xorcery.concurrent.CompletableFutures;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
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
import java.util.function.BiConsumer;

@Service(name = "crucible")
@RunLevel(20)
public class Crucible {

    private final Logger logger;
    private final Cabinet cabinet;
    private final CompletableFuture<Void> result;

    @Inject
    public Crucible(Configuration configuration, Cabinet cabinet, Xorcery xorcery, Logger logger) {
        this.cabinet = cabinet;
        this.logger = logger;
        this.result = new CompletableFuture<>();
        logger.info("Starting Crucible");

        CrucibleConfiguration crucibleConfiguration = CrucibleConfiguration.get(configuration);
        if (crucibleConfiguration.isCloseWhenDone())
            result.whenCompleteAsync((r, t) -> {
                if (t == null) xorcery.close();
                else xorcery.close(t);
            });
        crucibleConfiguration.getRecipes().forEach(recipe ->
                addTransmutation(recipe).whenComplete(CompletableFutures.transfer(result)));
    }

    public CompletableFuture<Void> addTransmutation(RecipeConfiguration transmutation) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        logger.info("Starting transmutation: " + transmutation.getName());
        Flux<MetadataJsonNode<JsonNode>> sourceFlux = getSource(transmutation);
        Flux<MetadataJsonNode<JsonNode>> transmutedFlux = applyTransmutes(transmutation, sourceFlux);
        Flux<MetadataJsonNode<JsonNode>> resultFlux = applyResult(transmutation, transmutedFlux);
        resultFlux.retryWhen(getRetry(transmutation));
        resultFlux.subscribe(item -> {
        }, result::completeExceptionally, () -> result.complete(null));
        result.whenCompleteAsync(this.logResult(transmutation.getName()));
        return result;
    }

    private BiConsumer<Void, Throwable> logResult(String name) {
        return (result, throwable) ->
        {
            if (throwable == null) {
                logger.info("Finished transmutation: " + name);
            } else {
                logger.error("Transmutation failed: " + name, throwable);
            }
        };
    }

    public CompletableFuture<Void> getResult() {
        return result;
    }

    private Flux<MetadataJsonNode<JsonNode>> getSource(RecipeConfiguration transmutation) {
        JarConfiguration inputConfiguration = transmutation.getSource();
        SourceJar inputPlugin = cabinet.getSourceJar(inputConfiguration.getJar()).orElseThrow(() -> new IllegalArgumentException("No source jar named:" + inputConfiguration.getJar()));
        Flux<MetadataJsonNode<JsonNode>> sourceFlux = inputPlugin.newSource(inputConfiguration.configuration(), transmutation);
        return sourceFlux;
    }

    private Flux<MetadataJsonNode<JsonNode>> applyTransmutes(RecipeConfiguration transmutation, Flux<MetadataJsonNode<JsonNode>> transmutedFlux) {
        for (JarConfiguration transmuteConfiguration : transmutation.getTransmutes()) {
            TransmuteJar transmuteJar = cabinet.getTransmuteJar(transmuteConfiguration.getJar()).orElseThrow(() -> new IllegalArgumentException("No transmute jar named:" + transmuteConfiguration.getJar()));
            transmutedFlux = transmutedFlux.transformDeferredContextual(transmuteJar.newIngredient(transmuteConfiguration.configuration(), transmutation));
        }
        return transmutedFlux;
    }

    private Flux<MetadataJsonNode<JsonNode>> applyResult(RecipeConfiguration transmutation, Flux<MetadataJsonNode<JsonNode>> transmutedFlux) {
        JarConfiguration resultConfiguration = transmutation.getResult();
        ResultJar resultJar = cabinet.getResultJar(resultConfiguration.getJar()).orElseThrow(() -> new IllegalArgumentException("No result jar named:" + resultConfiguration.getJar()));
        return transmutedFlux.transformDeferredContextual(resultJar.newResult(resultConfiguration.configuration(), transmutation));
    }

    private Retry getRetry(RecipeConfiguration recipeConfiguration) {
        return Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10));
    }
}
