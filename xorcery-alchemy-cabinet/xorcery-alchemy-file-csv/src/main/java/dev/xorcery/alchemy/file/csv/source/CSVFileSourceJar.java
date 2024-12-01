package dev.xorcery.alchemy.file.csv.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

@Service(name = "csv")
public class CSVFileSourceJar
        implements SourceJar {
    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        return Flux.from(new CSVPublisher(configuration, recipeConfiguration));
    }
}
