package dev.xorcery.alchemy.crucible;

import dev.xorcery.alchemy.jar.Jar;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class Cabinet {

    private final IterableProvider<Jar> jars;
    private final CabinetConfiguration cabinetConfiguration;

    @Inject
    public Cabinet(IterableProvider<Jar> jars, Configuration configuration, Logger logger) {
        this.jars = jars;
        this.cabinetConfiguration = CabinetConfiguration.get(configuration);

        logger.info("Source jars:" + String.join(",", StreamSupport.stream(jars.handleIterator().spliterator(), false)
                .filter(h -> SourceJar.class.isAssignableFrom(h.getActiveDescriptor().getImplementationClass()))
                .map(this::getJarName).toList()));
        logger.info("Transmute jars:" + String.join(",", StreamSupport.stream(jars.handleIterator().spliterator(), false)
                .filter(h -> TransmuteJar.class.isAssignableFrom(h.getActiveDescriptor().getImplementationClass()))
                .map(this::getJarName).toList()));
    }

    public Optional<SourceJar> getSourceJar(String jarName) {

        for (ServiceHandle<Jar> jar : jars.handleIterator()) {
            if (getJarName(jar).equals(jarName) &&
                    SourceJar.class.isAssignableFrom(jar.getActiveDescriptor().getImplementationClass())) {
                return Optional.of((SourceJar) jar.getService());
            }
        }
        return Optional.empty();
    }

    public Optional<JarConfiguration> getSourceJarConfiguration(String jarName){
        return cabinetConfiguration.getSourceJar(jarName);
    }

    public Optional<TransmuteJar> getTransmuteJar(String jarName) {

        for (ServiceHandle<Jar> jar : jars.handleIterator()) {
            if (getJarName(jar).equals(jarName) &&
                    TransmuteJar.class.isAssignableFrom(jar.getActiveDescriptor().getImplementationClass())) {
                return Optional.of((TransmuteJar) jar.getService());
            }
        }
        return Optional.empty();
    }

    public Optional<JarConfiguration> getTransmuteJarConfiguration(String jarName){
        return cabinetConfiguration.getTransmuteJar(jarName);
    }

    private String getJarName(ServiceHandle<Jar> serviceHandle) {
        return serviceHandle.getActiveDescriptor().getName();
    }
}
