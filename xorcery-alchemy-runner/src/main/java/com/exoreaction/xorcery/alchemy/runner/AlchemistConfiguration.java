package com.exoreaction.xorcery.alchemy.runner;

import com.exoreaction.xorcery.alchemy.plugin.TransmutationConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record AlchemistConfiguration(Configuration configuration) {
    public static AlchemistConfiguration get(Configuration configuration) {
        return new AlchemistConfiguration(configuration.getConfiguration("alchemist"));
    }

    public List<TransmutationConfiguration> getTransmutations() {
        return configuration.getObjectListAs("transmutations", json -> new TransmutationConfiguration(new Configuration(json)))
                .orElse(Collections.emptyList());
    }
}
