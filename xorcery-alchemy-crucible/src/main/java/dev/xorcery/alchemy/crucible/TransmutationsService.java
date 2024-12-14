package dev.xorcery.alchemy.crucible;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
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
public class TransmutationsService
        implements PreDestroy {
    private final List<CompletableFuture<Void>> transmutationResults = new ArrayList<>();
    private final CompletableFuture<Void> done;

    @Inject
    public TransmutationsService(Configuration configuration, Crucible crucible, Xorcery xorcery) {
        this(CrucibleConfiguration.get(configuration), crucible, xorcery);
    }

    public TransmutationsService(CrucibleConfiguration crucibleConfiguration, Crucible crucible, Xorcery xorcery) {
        crucibleConfiguration.getTransmutations().forEach(transmutation -> {
            if (transmutation.isEnabled()) {
                TransmutationConfiguration finalTransmutation = crucibleConfiguration.getRecipe(transmutation.getRecipe().getName().orElse(null))
                        .map(existingRecipe -> new TransmutationConfiguration(new Configuration(JsonNodeFactory.instance.objectNode()
                                .<ObjectNode>set("context", transmutation.getContext())
                                .set("recipe", existingRecipe.configuration().json()))))
                        .orElse(transmutation);
                transmutationResults.add(crucible.addTransmutation(finalTransmutation));
            }
        });

        done = CompletableFuture.allOf(transmutationResults.toArray(new CompletableFuture[0]));

        if (crucibleConfiguration.isCloseWhenDone())
            done.whenCompleteAsync((r, t) -> {
                if (t == null) xorcery.close();
                else xorcery.close(t);
            });
    }

    public CompletableFuture<Void> getResult() {
        return done;
    }

    @Override
    public void preDestroy() {
        for (CompletableFuture<Void> transmutationResult : transmutationResults) {
            transmutationResult.cancel(true);
        }
    }
}
