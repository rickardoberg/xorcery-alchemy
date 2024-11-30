package dev.xorcery.alchemy.crucible;


import dev.xorcery.alchemy.jar.Cabinet;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.Transmutation;
import dev.xorcery.concurrent.CompletableFutures;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.core.Xorcery;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service(name = "crucible")
@RunLevel(20)
public class Crucible
        implements PreDestroy {

    private final Logger logger;
    private final Cabinet cabinet;
    private final CompletableFuture<Void> result;
    private final List<CompletableFuture<Void>> transmutations = new CopyOnWriteArrayList<>();

    @Inject
    public Crucible(Configuration configuration, Cabinet cabinet, Xorcery xorcery, Logger logger) {
        this(CrucibleConfiguration.get(configuration), cabinet, xorcery, logger);
    }

    public Crucible(CrucibleConfiguration crucibleConfiguration, Cabinet cabinet, Xorcery xorcery, Logger logger) {
        this.cabinet = cabinet;
        this.logger = logger;
        this.result = new CompletableFuture<>();
        logger.info("Starting Crucible");

        if (crucibleConfiguration.isCloseWhenDone())
            result.whenCompleteAsync((r, t) -> {
                if (t == null) xorcery.close();
                else xorcery.close(t);
            });
        crucibleConfiguration.getRecipes().forEach(recipe -> {
                    if (recipe.isEnabled()) {
                        addTransmutation(recipe).whenComplete(CompletableFutures.transfer(result));
                    }
                }
        );
    }

    public CompletableFuture<Void> addTransmutation(RecipeConfiguration recipeConfiguration) {
        CompletableFuture<Void> result = new CompletableFuture<Void>();
        result.whenCompleteAsync(logResult(recipeConfiguration.getName().orElse("crucible")));
        try {
            logger.info("Starting transmutation: " + recipeConfiguration.getName());
            Transmutation transmutation = cabinet.newTransmutation(recipeConfiguration);
            Disposable disposable = transmutation.getFlux().subscribe(item -> {
            }, result::completeExceptionally, () -> result.complete(null));
            result.exceptionally(disposeOnCancel(disposable));
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
}
