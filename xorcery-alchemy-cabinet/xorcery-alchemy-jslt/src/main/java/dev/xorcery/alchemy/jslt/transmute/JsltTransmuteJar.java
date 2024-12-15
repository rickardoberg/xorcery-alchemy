package dev.xorcery.alchemy.jslt.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.Parser;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.TransmutationConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Service(name = "jslt", metadata = "enabled=jars.enabled")
public class JsltTransmuteJar
        implements TransmuteJar {

    private final Collection<Function> customFunctions = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public JsltTransmuteJar(Logger logger) {
        ServiceLoader<Function> functions = ServiceLoader.load(Function.class);
        functions.forEach(customFunctions::add);
        if (!customFunctions.isEmpty())
            logger.info("Custom functions: "+String.join(",", customFunctions.stream().map(Function::getName).toList()));
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {

        return jarConfiguration.configuration().getJson("jslt").<BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>>>map(jslt ->
                {
                    Expression expression = Parser.compileString(jslt.asText(), customFunctions);
                    return newJsltTransmuteFactory(expression, jarConfiguration);
                }
        ).orElse((metadataJsonNodeFlux, contextView) -> Flux.error(new IllegalArgumentException("Missing 'jslt' transformation configuration")));
    }

    BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newJsltTransmuteFactory(Expression expression, JarConfiguration jarConfiguration)
    {
        return (flux, context)->
        {
            Map<String, JsonNode> variables = new HashMap<>();
            jarConfiguration.configuration().getConfiguration("context").object().fields().forEachRemaining(entry ->
            {
                variables.put(entry.getKey(), entry.getValue());
            });
            return flux.handle(new JsltTransmute(expression, variables));
        };
    }

    static class JsltTransmute
            implements BiConsumer<MetadataJsonNode<JsonNode>, SynchronousSink<MetadataJsonNode<JsonNode>>> {
        private final Expression expression;
        private final Map<String, JsonNode> variables;

        public JsltTransmute(Expression expression, Map<String, JsonNode> variables) {
            this.expression = expression;
            this.variables = variables;
        }

        @Override
        public void accept(MetadataJsonNode<JsonNode> item, SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
            try {
                ObjectNode metaDataJson = JsonNodeFactory.instance.objectNode();
                metaDataJson.set("metadata", item.metadata().json());
                metaDataJson.set("data", item.data());
                JsonNode output = expression.apply(variables, metaDataJson);
                if (!output.isNull())
                {
                    ObjectNode metadata = (ObjectNode) output.get("metadata");
                    if (metadata == null)
                    {
                        metadata = JsonNodeFactory.instance.objectNode();
                    }
                    JsonNode data = output.get("data");
                    if (data == null)
                    {
                        data = JsonNodeFactory.instance.objectNode();
                    }
                    item.set(new Metadata(metadata), data);
                    sink.next(item);
                }
            } catch (Exception e) {
                sink.error(e);
            }
        }
    }
}
