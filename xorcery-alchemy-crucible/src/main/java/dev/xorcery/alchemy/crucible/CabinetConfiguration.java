package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CabinetConfiguration(List<JarConfiguration> sourceJars, List<JarConfiguration> transmuteJars) {

    public static CabinetConfiguration get(Configuration configuration)
    {
        return new CabinetConfiguration(configuration.getObjectListAs("jars.sourcejars",
                json ->new JarConfiguration(new Configuration(json))).orElse(Collections.emptyList()),
                configuration.getObjectListAs("jars.transmutejars",
                        json ->new JarConfiguration(new Configuration(json))).orElse(Collections.emptyList()));
    }

    public Optional<JarConfiguration> getSourceJar(String name) {
        return sourceJars.stream()
                .filter(sourceJar -> Objects.equals(sourceJar.getName().orElse(null), name))
                .findFirst();
    }

    public Optional<JarConfiguration> getTransmuteJar(String name) {
        return transmuteJars.stream()
                .filter(transmuteJar -> Objects.equals(transmuteJar.getName().orElse(null), name))
                .findFirst();
    }
}
