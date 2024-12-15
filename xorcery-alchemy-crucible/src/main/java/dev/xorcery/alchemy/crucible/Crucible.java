package dev.xorcery.alchemy.crucible;


import dev.xorcery.alchemy.jar.Transmutation;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service(name = "crucible")
public class Crucible implements PreDestroy {

    private final Logger logger;

    private final List<CompletableFuture<Void>> transmutations = new CopyOnWriteArrayList<>();

    @Inject
    public Crucible(Logger logger) {
        this.logger = logger;
        logger.info("Starting Crucible");
    }

    public CompletableFuture<Void> addTransmutation(Transmutation transmutation) {
        CompletableFuture<Void> result = new CompletableFuture<Void>();
        TransmutationConfiguration transmutationConfiguration = transmutation.getTransmutationConfiguration();
        result.whenCompleteAsync(logResult(transmutationConfiguration.getName().or(transmutationConfiguration::getRecipe).orElse("crucible")));
        try {
            transmutationConfiguration.getName().or(transmutationConfiguration::getRecipe).ifPresent(name -> logger.info("Starting: " + name));
            transmutations.add(result);
            result.whenComplete((r, t) -> transmutations.remove(result));
            Disposable disposable = transmutation.getFlux().subscribe(item -> {
            }, result::completeExceptionally, () -> result.complete(null));
            result.exceptionally(disposeOnCancel(disposable));
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    public List<CompletableFuture<Void>> getTransmutations() {
        return Collections.unmodifiableList(new ArrayList<>(transmutations));
    }

    public CompletableFuture<Void> getAllTransmutations()
    {
        return CompletableFuture.allOf(transmutations.toArray(new CompletableFuture[0]));
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
                logger.info(MarkerManager.getMarker(name), "Finished: " + name);
            } else {
                logger.error(MarkerManager.getMarker(name), "Failed: " + name, throwable);
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
}
