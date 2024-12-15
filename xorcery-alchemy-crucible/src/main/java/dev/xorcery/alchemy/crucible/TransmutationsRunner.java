package dev.xorcery.alchemy.crucible;

import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service(name = "transmutations", metadata = "enabled=crucible.enabled")
@RunLevel(20)
public class TransmutationsRunner {

    private final CompletableFuture<Void> done;

    @Inject
    public TransmutationsRunner(Configuration configuration, Transmutations transmutations, Crucible crucible) {
        this(TransmutationsConfiguration.get(configuration), transmutations, crucible);
    }

    public TransmutationsRunner(TransmutationsConfiguration transmutationsConfiguration, Transmutations transmutations, Crucible crucible) {
        List<CompletableFuture<Void>> results = new ArrayList<>();
        transmutationsConfiguration.getTransmutations().forEach(transmutation -> {
            if (transmutation.isEnabled()) {
                results.add(crucible.addTransmutation(transmutations.newTransmutation(transmutation)));
            }
        });
        done = CompletableFuture.allOf(results.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> getDone() {
        return done;
    }
}
