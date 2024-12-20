package dev.xorcery.alchemy.common.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.crucible.Transmutations;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Service(name="concat", metadata = "enabled=jars.enabled")
public class ConcatSourceJar
    implements SourceJar
{
    private final Transmutations transmutations;

    @Inject
    public ConcatSourceJar(Transmutations transmutations) {
        this.transmutations = transmutations;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration configuration, TransmutationConfiguration transmutationConfiguration) {
        return configuration.configuration().getObjectListAs("sources", source -> new JarConfiguration(new Configuration(source))).map(sources ->
        {
            List<Flux<MetadataJsonNode<JsonNode>>> sourceFluxes = sources.stream()
                    .map(source -> transmutations.newSourceFlux(source, transmutationConfiguration))
                    .filter(Optional::isPresent)
                    .map(Optional::get).toList();
            return Flux.concat(sourceFluxes);
        }).orElse(Flux.empty());
    }
}
