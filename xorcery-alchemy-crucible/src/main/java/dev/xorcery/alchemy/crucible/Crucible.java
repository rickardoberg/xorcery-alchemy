package dev.xorcery.alchemy.crucible;


import dev.xorcery.alchemy.jar.Cabinet;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.Transmutation;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service(name = "crucible")
public class Crucible {

    private final Logger logger;
    private final Cabinet cabinet;

    @Inject
    public Crucible(Cabinet cabinet, Logger logger) {
        this.cabinet = cabinet;
        this.logger = logger;
        logger.info("Starting Crucible");
    }

    public CompletableFuture<Void> addTransmutation(RecipeConfiguration recipeConfiguration) {
        CompletableFuture<Void> result = new CompletableFuture<Void>();
        result.whenCompleteAsync(logResult(recipeConfiguration.getName().orElse("crucible")));
        try {
            recipeConfiguration.getName().ifPresent(name -> logger.info("Starting transmutation: " + name));
            Transmutation transmutation = cabinet.newTransmutation(recipeConfiguration);
            Disposable disposable = transmutation.getFlux().subscribe(item -> {
            }, result::completeExceptionally, () -> result.complete(null));
            result.exceptionally(disposeOnCancel(disposable));
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
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
}
