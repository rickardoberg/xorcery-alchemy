package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record CrucibleConfiguration(Configuration configuration) {
    public static CrucibleConfiguration get(Configuration configuration) {
        return new CrucibleConfiguration(configuration.getConfiguration("crucible"));
    }

    public boolean isCloseWhenDone() {
        return configuration.getBoolean("closeWhenDone").orElse(true);
    }

    public List<TransmutationConfiguration> getTransmutations() {
        return configuration.getObjectListAs("transmutations", json -> new TransmutationConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }
}
