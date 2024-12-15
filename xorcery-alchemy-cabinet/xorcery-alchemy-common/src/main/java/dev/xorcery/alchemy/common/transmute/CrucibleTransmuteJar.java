package dev.xorcery.alchemy.common.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.crucible.Crucible;
import dev.xorcery.alchemy.crucible.Transmutations;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

@Service(name = "crucible", metadata = "enabled=jars.enabled")
public class CrucibleTransmuteJar
        implements TransmuteJar {
    private final Transmutations transmutations;
    private final Crucible crucible;

    @Inject
    public CrucibleTransmuteJar(Transmutations transmutations, Crucible crucible) {
        this.transmutations = transmutations;
        this.crucible = crucible;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {
        return (flux, context) -> flux.handle((item, sink) ->
        {
            try {
                if (item.data() instanceof ObjectNode transmutationJson) {
                    crucible.addTransmutation(transmutations.newTransmutation(new TransmutationConfiguration(new Configuration(transmutationJson))));
                }
                sink.next(item);
            } catch (Throwable t) {
                sink.error(t);
            }
        });
    }
}
