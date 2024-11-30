package dev.xorcery.alchemy.test;

import dev.xorcery.alchemy.crucible.Crucible;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class FlattenTest {

    @Test
    public void testFlatten() throws Exception {
        try (Xorcery crucible = new Xorcery(new ConfigurationBuilder()
                .addTestDefaults()
                .addResource("testFlatten.yaml")
                .build()))
        {
            crucible.getServiceLocator().getService(Crucible.class).getResult().orTimeout(10, TimeUnit.SECONDS).join();
        }
    }
}
