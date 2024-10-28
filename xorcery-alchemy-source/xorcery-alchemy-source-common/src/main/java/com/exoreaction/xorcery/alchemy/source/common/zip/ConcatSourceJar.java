package com.exoreaction.xorcery.alchemy.source.common.zip;

import com.exoreaction.xorcery.alchemy.jar.Cabinet;
import com.exoreaction.xorcery.alchemy.jar.JarConfiguration;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.alchemy.jar.SourceJar;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Service(name="jars.concat")
public class ConcatSourceJar
    implements SourceJar
{
    private final Cabinet cabinet;

    @Inject
    public ConcatSourceJar(Cabinet cabinet) {
        this.cabinet = cabinet;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(Configuration configuration, RecipeConfiguration recipeConfiguration) {
        return configuration.getObjectListAs("sources", source -> new JarConfiguration(new Configuration(source))).map(sources ->
        {
            List<Flux<MetadataJsonNode<JsonNode>>> sourceFluxes = sources.stream()
                    .map(source -> cabinet.getSourceJar(source.getJar()).map(jar -> jar.newSource(source.configuration(), recipeConfiguration)))
                    .filter(Optional::isPresent)
                    .map(Optional::get).toList();
            return Flux.concat(sourceFluxes);
        }).orElse(Flux.empty());
    }
}
