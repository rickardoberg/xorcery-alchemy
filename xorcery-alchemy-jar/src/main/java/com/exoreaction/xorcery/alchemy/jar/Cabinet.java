package com.exoreaction.xorcery.alchemy.jar;

import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.util.Map;
import java.util.Optional;
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

    public Optional<Flux<MetadataJsonNode<JsonNode>>> newSourceFlux(JarConfiguration sourceConfiguration, RecipeConfiguration recipeConfiguration)
    {
        return getSourceJar(sourceConfiguration.getJar())
                .map(sourceJar ->
                {
                    Flux<MetadataJsonNode<JsonNode>> sourceFlux = sourceJar.newSource(sourceConfiguration, recipeConfiguration);
                    Map<String, Object> context = sourceConfiguration.getContext();
                    if (!context.isEmpty())
                    {
                        sourceFlux = sourceFlux.contextWrite(Context.of(context));
                    }
                    return sourceFlux;
                });
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


    public Optional<Flux<MetadataJsonNode<JsonNode>>> applyTransmuteFlux(Flux<MetadataJsonNode<JsonNode>> flux, JarConfiguration transmuteConfiguration, RecipeConfiguration recipeConfiguration) {
        return getTransmuteJar(transmuteConfiguration.getJar()).map(transmuteJar ->
        {
            Flux<MetadataJsonNode<JsonNode>> transmutedFlux = flux.transformDeferredContextual(transmuteJar.newTransmute(transmuteConfiguration, recipeConfiguration));
            Map<String, Object> context = transmuteConfiguration.getContext();
            if (!context.isEmpty())
            {
                transmutedFlux = transmutedFlux.contextWrite(Context.of(context));
            }
            return transmutedFlux;
        });
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

    public <T> Optional<Flux<MetadataJsonNode<JsonNode>>> applyResultFlux(Flux<MetadataJsonNode<JsonNode>> transmutedFlux, JarConfiguration resultConfiguration, RecipeConfiguration recipeConfiguration) {
        return getResultJar(resultConfiguration.getJar())
                .map(resultJar ->
                {
                    Flux<MetadataJsonNode<JsonNode>> resultFlux = transmutedFlux.transformDeferredContextual(resultJar.newResult(resultConfiguration, recipeConfiguration));
                    Map<String, Object> context = resultConfiguration.getContext();
                    if (!context.isEmpty())
                    {
                        resultFlux = resultFlux.contextWrite(Context.of(context));
                    }
                    return resultFlux;
                });
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
