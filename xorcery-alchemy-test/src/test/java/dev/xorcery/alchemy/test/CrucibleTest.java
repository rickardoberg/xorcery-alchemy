package dev.xorcery.alchemy.test;

import dev.xorcery.alchemy.crucible.Crucible;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class CrucibleTest {

    @Test
    public void testCrucible() throws Exception {
        try (Xorcery crucible = new Xorcery(new ConfigurationBuilder()
                .addTestDefaults()
                .addResource("testCrucible.yaml")
                .build()))
        {
            crucible.getServiceLocator().getService(Crucible.class).getResult().orTimeout(10, TimeUnit.SECONDS).join();
        }
    }
}