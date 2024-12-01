package dev.xorcery.alchemy.file.excel.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

@Service(name = "excel")
public class ExcelSourceJar
        implements SourceJar
{
    private final Configuration configuration;

    @Inject
    public ExcelSourceJar(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        return Flux.from(new ExcelPublisher(jarConfiguration, recipeConfiguration));
    }
}
