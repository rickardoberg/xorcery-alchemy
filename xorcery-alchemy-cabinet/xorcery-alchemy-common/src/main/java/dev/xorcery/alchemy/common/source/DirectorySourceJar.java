package dev.xorcery.alchemy.common.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

@Service(name="directory")
public class DirectorySourceJar
    implements SourceJar
{
    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        return null;
    }
}
