package com.exoreaction.xorcery.alchemy.jar;

import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class Cabinet {

    private final IterableProvider<Jar> jars;

    @Inject
    public Cabinet(IterableProvider<Jar> jars, Logger logger) {
        this.jars = jars;

        logger.info("Jars:" + String.join(",", StreamSupport.stream(jars.handleIterator().spliterator(), false).map(this::getJarName).toList()));
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

    public Optional<TransmuteJar> getTransmuteJar(String jarName) {

        for (ServiceHandle<Jar> jar : jars.handleIterator()) {
            if (getJarName(jar).equals(jarName) &&
                    TransmuteJar.class.isAssignableFrom(jar.getActiveDescriptor().getImplementationClass())) {
                return Optional.of((TransmuteJar) jar.getService());
            }
        }
        return Optional.empty();
    }

    public Optional<ResultJar> getResultJar(String jarName) {

        for (ServiceHandle<Jar> jar : jars.handleIterator()) {
            if (getJarName(jar).equals(jarName) &&
                    ResultJar.class.isAssignableFrom(jar.getActiveDescriptor().getImplementationClass())) {
                return Optional.of((ResultJar) jar.getService());
            }
        }
        return Optional.empty();
    }

    private String getJarName(String serviceName) {
        if (serviceName == null)
            return null;
        int lastDotIndex = serviceName.lastIndexOf('.');
        return lastDotIndex == -1 ? serviceName : serviceName.substring(lastDotIndex + 1);
    }

    private String getJarName(ServiceHandle<Jar> serviceHandle) {
        return getJarName(serviceHandle.getActiveDescriptor().getName());
    }
}
