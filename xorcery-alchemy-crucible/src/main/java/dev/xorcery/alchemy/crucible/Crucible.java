package dev.xorcery.alchemy.crucible;


import dev.xorcery.alchemy.jar.Cabinet;
import dev.xorcery.alchemy.jar.Transmutation;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
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
public class Crucible {

    private final Logger logger;
    private final Cabinet cabinet;

    private final List<CompletableFuture<Void>> transmutations = new CopyOnWriteArrayList<>();

    @Inject
    public Crucible(Cabinet cabinet, Logger logger) {
        this.cabinet = cabinet;
        this.logger = logger;
        logger.info("Starting Crucible");
    }

    public CompletableFuture<Void> addTransmutation(TransmutationConfiguration transmutationConfiguration) {
        CompletableFuture<Void> result = new CompletableFuture<Void>();
        result.whenCompleteAsync(logResult(transmutationConfiguration.getName().or(()->transmutationConfiguration.getRecipe().getName()).orElse("crucible")));
        try {
            transmutationConfiguration.getName().or(()->transmutationConfiguration.getRecipe().getName()).ifPresent(name -> logger.info("Starting: " + name));
            Transmutation transmutation = cabinet.newTransmutation(transmutationConfiguration);
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
