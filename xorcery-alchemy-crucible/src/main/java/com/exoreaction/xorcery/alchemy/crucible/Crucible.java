package com.exoreaction.xorcery.alchemy.crucible;


import com.exoreaction.xorcery.alchemy.jar.Cabinet;
import com.exoreaction.xorcery.alchemy.jar.JarConfiguration;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.concurrent.CompletableFutures;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service(name = "crucible")
@RunLevel(20)
public class Crucible
    implements PreDestroy
{

    private final Logger logger;
    private final Cabinet cabinet;
    private final CompletableFuture<Void> result;
    private final List<CompletableFuture<Void>> transmutations = new CopyOnWriteArrayList<>();

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

    public CompletableFuture<Void> addTransmutation(RecipeConfiguration recipeConfiguration) {
        CompletableFuture<Void> result = new CompletableFuture<Void>().whenCompleteAsync(logResult(recipeConfiguration.getName()));
        try {
            logger.info("Starting transmutation: " + recipeConfiguration.getName());
            Flux<MetadataJsonNode<JsonNode>> sourceFlux = newSourceFlux(recipeConfiguration);
            Flux<MetadataJsonNode<JsonNode>> transmutedFlux = applyTransmutes(sourceFlux, recipeConfiguration);
            Flux<MetadataJsonNode<JsonNode>> resultFlux = applyResult(transmutedFlux, recipeConfiguration);
            resultFlux = resultFlux.retryWhen(getRetry(recipeConfiguration));
            Map<String, Object> context = recipeConfiguration.getContext();
            if (!context.isEmpty())
            {
                resultFlux = resultFlux.contextWrite(Context.of(context));
            }
            Disposable disposable = resultFlux.subscribe(item -> {
            }, result::completeExceptionally, () -> result.complete(null));
            transmutations.add(result);
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    public List<CompletableFuture<Void>> getTransmutations() {
        return transmutations;
    }

    public CompletableFuture<Void> getResult() {
        return result;
    }

    @Override
    public void preDestroy() {
        for (CompletableFuture<Void> transmutation : transmutations) {
            transmutation.cancel(true);
        }
    }

    private BiConsumer<? super Void, Throwable> logResult(String name) {
        return (result, throwable) ->
        {
            if (throwable == null) {
                logger.info("Finished transmutation: " + name);
            } else {
                logger.error("Transmutation failed: " + name, throwable);
            }
        };
    }

    private Function<Throwable, Void> disposeOnCancel(Disposable disposable) {
        return throwable -> {
            if (throwable instanceof CancellationException)
                disposable.dispose();
            return null;
        };
    }

    private Flux<MetadataJsonNode<JsonNode>> newSourceFlux(RecipeConfiguration recipeConfiguration) {
        JarConfiguration sourceConfiguration = recipeConfiguration.getSource();
        return cabinet.newSourceFlux(sourceConfiguration, recipeConfiguration)
                .orElseThrow(() -> new IllegalArgumentException("No source jar named:" + sourceConfiguration.getJar()));
    }

    private Flux<MetadataJsonNode<JsonNode>> applyTransmutes(Flux<MetadataJsonNode<JsonNode>> transmutedFlux, RecipeConfiguration recipeConfiguration) {
        for (JarConfiguration transmuteConfiguration : recipeConfiguration.getTransmutes()) {
            transmutedFlux = cabinet.applyTransmuteFlux(transmutedFlux, transmuteConfiguration, recipeConfiguration)
                    .orElseThrow(() -> new IllegalArgumentException("No transmute jar named:" + transmuteConfiguration.getJar()));
        }
        return transmutedFlux;
    }

    private Flux<MetadataJsonNode<JsonNode>> applyResult(Flux<MetadataJsonNode<JsonNode>> transmutedFlux, RecipeConfiguration recipeConfiguration) {
        JarConfiguration resultConfiguration = recipeConfiguration.getResult();
        return cabinet.applyResultFlux(transmutedFlux, resultConfiguration, recipeConfiguration)
                .orElseThrow(() -> new IllegalArgumentException("No result jar named:" + resultConfiguration.getJar()));
    }

    private Retry getRetry(RecipeConfiguration recipeConfiguration) {
        return Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10))
                .filter(this::isRetryable);
    }

    private boolean isRetryable(Throwable throwable) {
        return false;
    }
}
