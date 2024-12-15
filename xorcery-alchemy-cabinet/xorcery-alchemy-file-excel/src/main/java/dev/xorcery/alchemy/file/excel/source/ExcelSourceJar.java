package dev.xorcery.alchemy.file.excel.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

@Service(name = "excel", metadata = "enabled=jars.enabled")
public class ExcelSourceJar
        implements SourceJar
{
    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {
        return Flux.from(new ExcelPublisher(jarConfiguration, transmutationConfiguration));
    }
}
