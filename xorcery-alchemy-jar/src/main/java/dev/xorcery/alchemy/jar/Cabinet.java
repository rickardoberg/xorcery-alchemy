package dev.xorcery.alchemy.jar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.json.JsonElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class Cabinet {

    private final IterableProvider<Jar> jars;

    @Inject
    public Cabinet(IterableProvider<Jar> jars, Logger logger) {
        this.jars = jars;

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

    public Optional<Flux<MetadataJsonNode<JsonNode>>> newSourceFlux(JarConfiguration sourceConfiguration, RecipeConfiguration recipeConfiguration) {
        return getSourceJar(sourceConfiguration.getJar())
                .map(sourceJar ->
                {
                    Flux<MetadataJsonNode<JsonNode>> sourceFlux = sourceJar.newSource(sourceConfiguration, recipeConfiguration);
                    Map<String, Object> context = sourceConfiguration.getContext();
                    if (!context.isEmpty()) {
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
            if (!context.isEmpty()) {
                transmutedFlux = transmutedFlux.contextWrite(Context.of(context));
            }
            return transmutedFlux;
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

    public Transmutation newTransmutation(TransmutationConfiguration transmutationConfiguration) {

        Flux<MetadataJsonNode<JsonNode>> sourceFlux = newSourceFlux(transmutationConfiguration);
        Flux<MetadataJsonNode<JsonNode>> transmutedFlux = applyTransmutes(sourceFlux, transmutationConfiguration);
        return new Transmutation(transmutationConfiguration, transmutedFlux);
    }

    private Flux<MetadataJsonNode<JsonNode>> newSourceFlux(TransmutationConfiguration transmutationConfiguration) {
        JarConfiguration sourceConfiguration = transmutationConfiguration.getRecipe().getSource();
        return newSourceFlux(sourceConfiguration, transmutationConfiguration.getRecipe())
                .orElseThrow(() -> new IllegalArgumentException("No source jar named:" + sourceConfiguration.getJar()));
    }

    private Flux<MetadataJsonNode<JsonNode>> applyTransmutes(Flux<MetadataJsonNode<JsonNode>> transmutedFlux, TransmutationConfiguration transmutationConfiguration) {
        List<JarConfiguration> transmutes = transmutationConfiguration.getRecipe().getTransmutes();
        for (JarConfiguration transmuteConfiguration : transmutes) {
            if (transmuteConfiguration.isEnabled()) {
                transmutedFlux = applyTransmuteFlux(transmutedFlux, transmuteConfiguration, transmutationConfiguration.getRecipe())
                        .orElseThrow(() -> new IllegalArgumentException("No transmute jar named:" + transmuteConfiguration.getJar()));
            }
        }
        ObjectNode contextConfiguration = transmutationConfiguration.getContext();
        if (!contextConfiguration.isEmpty())
        {
            transmutedFlux = transmutedFlux.contextWrite(Context.of(JsonElement.toMap(contextConfiguration)));
        }
        return transmutedFlux;
    }
}
