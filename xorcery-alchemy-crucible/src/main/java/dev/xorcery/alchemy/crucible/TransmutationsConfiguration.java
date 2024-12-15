package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record TransmutationsConfiguration(List<TransmutationConfiguration> transmutations) {
    public static TransmutationsConfiguration get(Configuration configuration){
        return new TransmutationsConfiguration(configuration.getObjectListAs("transmutations",
                        json -> new TransmutationConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList()));
    }

    public List<TransmutationConfiguration> getTransmutations() {
        return transmutations;
    }
}
