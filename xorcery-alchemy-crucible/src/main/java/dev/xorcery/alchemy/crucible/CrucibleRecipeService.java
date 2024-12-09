package dev.xorcery.alchemy.crucible;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.core.Xorcery;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service(name = "crucible")
@RunLevel(20)
public class CrucibleRecipeService
    implements PreDestroy
{
    private final List<CompletableFuture<Void>> transmutationResults = new ArrayList<>();
    private final CompletableFuture<Void> done;

    @Inject
    public CrucibleRecipeService(Configuration configuration, Crucible crucible, Xorcery xorcery) {
        this(CrucibleConfiguration.get(configuration), crucible, xorcery);
    }

    public CrucibleRecipeService(CrucibleConfiguration crucibleConfiguration, Crucible crucible, Xorcery xorcery) {
        crucibleConfiguration.getRecipes().forEach(recipe -> {
            if (recipe.isEnabled()) {
                transmutationResults.add(crucible.addTransmutation(recipe));
            }
        });

        done = CompletableFuture.allOf(transmutationResults.toArray(new CompletableFuture[0]));

        if (crucibleConfiguration.isCloseWhenDone())
            done.whenCompleteAsync((r, t) -> {
                if (t == null) xorcery.close();
                else xorcery.close(t);
            });
    }

    public CompletableFuture<Void> getResult()
    {
        return done;
    }

    @Override
    public void preDestroy() {
        for (CompletableFuture<Void> transmutationResult : transmutationResults) {
            transmutationResult.cancel(true);
        }
    }
}
