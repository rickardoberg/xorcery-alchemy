package dev.xorcery.alchemy.crucible;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.Transmutation;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonMerger;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service(name = "transmutations", metadata = "enabled=crucible.enabled")
public class Transmutations {
    private final Cabinet cabinet;
    private final Recipes recipes;

    @Inject
    public Transmutations(Configuration configuration, Cabinet cabinet, Recipes recipes) {
        this(TransmutationsConfiguration.get(configuration), cabinet, recipes);
    }

    public Transmutations(TransmutationsConfiguration transmutationsConfiguration, Cabinet cabinet, Recipes recipes) {
        this.cabinet = cabinet;
        this.recipes = recipes;
    }

    public Optional<Flux<MetadataJsonNode<JsonNode>>> newSourceFlux(JarConfiguration sourceConfiguration, TransmutationConfiguration recipeConfiguration) {
        return cabinet.getSourceJar(sourceConfiguration.getJar()).map(sourceJar ->
                sourceJar.newSource(sourceConfiguration, recipeConfiguration));
    }


    public Optional<Flux<MetadataJsonNode<JsonNode>>> applyTransmuteFlux(Flux<MetadataJsonNode<JsonNode>> flux, JarConfiguration transmuteConfiguration, TransmutationConfiguration transmutationConfiguration) {
        return cabinet.getTransmuteJar(transmuteConfiguration.getJar()).map(transmuteJar ->
                flux.transformDeferredContextual(transmuteJar.newTransmute(transmuteConfiguration, transmutationConfiguration)));
    }

    public Transmutation newTransmutation(final TransmutationConfiguration transmutationConfiguration) {
        // Add jar default configurations
        JarConfiguration sourceJarConfiguration = cabinet.getSourceJarConfiguration(transmutationConfiguration.getSource().getJar()).map(defaultConfig ->
                new JarConfiguration(new Configuration(new JsonMerger().merge(defaultConfig.configuration().json(), transmutationConfiguration.getSource().configuration().json()))))
                .orElse(transmutationConfiguration.getSource());
        List<ObjectNode> transmuteConfigurations = new ArrayList<>(transmutationConfiguration.getTransmutes().size());
        for (JarConfiguration transmute : transmutationConfiguration.getTransmutes()) {
            JarConfiguration transmuteWithDefaults = cabinet.getTransmuteJarConfiguration(transmute.getJar()).map(defaultConfig ->
                            new JarConfiguration(new Configuration(new JsonMerger().merge(defaultConfig.configuration().json(), transmute.configuration().json()))))
                    .orElse(transmute);
            transmuteConfigurations.add(transmuteWithDefaults.configuration().json());
        }
        ObjectNode transmutationJson = JsonNodeFactory.instance.objectNode();
        transmutationJson.set("source", sourceJarConfiguration.configuration().json());
        transmutationJson.set("transmutes", JsonNodeFactory.instance.arrayNode().addAll(transmuteConfigurations));
        transmutationConfiguration.getName().ifPresent(name -> transmutationJson.set("name", JsonNodeFactory.instance.textNode(name)));
        transmutationConfiguration.getRecipe().ifPresent(name -> transmutationJson.set("recipe", JsonNodeFactory.instance.textNode(name)));

        TransmutationConfiguration transmutationConfigurationWithDefaults = new TransmutationConfiguration(new Configuration(transmutationJson));

        // Add recipe configuration
        TransmutationConfiguration finalTransmutationConfiguration = recipes.getRecipeByName(transmutationConfigurationWithDefaults.getRecipe().orElse(null))
                .map(existingRecipe -> new TransmutationConfiguration(new Configuration(new JsonMerger().apply(existingRecipe.configuration().json(), transmutationConfigurationWithDefaults.configuration().json()))))
                .orElse(transmutationConfiguration);

        // Now instantiate it
        Flux<MetadataJsonNode<JsonNode>> sourceFlux = newSourceFlux(finalTransmutationConfiguration);
        sourceFlux = sourceFlux.publishOn(Schedulers.boundedElastic(), 512);
        Flux<MetadataJsonNode<JsonNode>> transmutedFlux = applyTransmutes(sourceFlux, transmutationConfiguration);
        return new Transmutation(transmutationConfiguration, transmutedFlux);
    }

    private Flux<MetadataJsonNode<JsonNode>> newSourceFlux(TransmutationConfiguration transmutationConfiguration) {
        JarConfiguration sourceConfiguration = transmutationConfiguration.getSource();
        return newSourceFlux(sourceConfiguration, transmutationConfiguration)
                .orElseThrow(() -> new IllegalArgumentException("No source jar named:" + sourceConfiguration.getJar()));
    }

    private Flux<MetadataJsonNode<JsonNode>> applyTransmutes(final Flux<MetadataJsonNode<JsonNode>> sourceFlux, TransmutationConfiguration transmutationConfiguration) {
        List<JarConfiguration> transmutes = transmutationConfiguration.getTransmutes();
        Flux<MetadataJsonNode<JsonNode>> transmutedFlux = sourceFlux;
        for (JarConfiguration transmuteConfiguration : transmutes) {
            if (transmuteConfiguration.isEnabled()) {
                transmutedFlux = applyTransmuteFlux(transmutedFlux, transmuteConfiguration, transmutationConfiguration)
                        .orElseThrow(() -> new IllegalArgumentException("No transmute jar named:" + transmuteConfiguration.getJar()));
            }
        }
        return transmutedFlux;
    }
}
