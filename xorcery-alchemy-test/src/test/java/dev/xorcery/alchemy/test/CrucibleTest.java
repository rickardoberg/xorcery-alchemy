package dev.xorcery.alchemy.test;

import dev.xorcery.alchemy.crucible.TransmutationsService;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

class CrucibleTest {

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(b -> b.addResource("testCrucible.yaml"))
            .build();

    @Test
    public void testCrucible(TransmutationsService transmutations) throws Exception {
        transmutations.getResult().orTimeout(10, TimeUnit.SECONDS).join();
    }
}