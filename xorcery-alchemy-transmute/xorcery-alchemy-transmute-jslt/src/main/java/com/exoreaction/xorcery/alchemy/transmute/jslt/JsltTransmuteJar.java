package com.exoreaction.xorcery.alchemy.transmute.jslt;

import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.alchemy.jar.TransmuteJar;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.Parser;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

@Service(name = "jars.jslt")
public class JsltTransmuteJar
        implements TransmuteJar {

    private final Collection<Function> customFunctions = new ArrayList<>();

    @Inject
    public JsltTransmuteJar(Logger logger) {
        ServiceLoader<Function> functions = ServiceLoader.load(Function.class);
        functions.forEach(customFunctions::add);
        if (!customFunctions.isEmpty())
            logger.info("Custom functions: "+String.join(",", customFunctions.stream().map(Function::getName).toList()));
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newIngredient(Configuration configuration, RecipeConfiguration recipeConfiguration) {

        return configuration.getJson("jslt").<BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>>>map(jslt ->
                {
                    Expression expression = Parser.compileString(jslt.asText(), customFunctions);
                    return new JsltTransmute(expression);
                }
        ).orElse((metadataJsonNodeFlux, contextView) -> Flux.error(new IllegalArgumentException("Missing 'jslt' transformation configuration")));
    }

    static class JsltTransmute
            implements BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> {
        private final Expression expression;

        public JsltTransmute(Expression expression) {
            this.expression = expression;
        }

        @Override
        public Publisher<MetadataJsonNode<JsonNode>> apply(Flux<MetadataJsonNode<JsonNode>> flux, ContextView contextView) {
            return flux.handle(this::convert);
        }

        private void convert(MetadataJsonNode<JsonNode> item, SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
            try {
                ObjectNode metaDataJson = JsonNodeFactory.instance.objectNode();
                metaDataJson.set("metadata", item.metadata().json());
                metaDataJson.set("data", item.data());
                JsonNode output = expression.apply(metaDataJson);
                item.set(new Metadata((ObjectNode)output.get("metadata")), output.get("data"));
                sink.next(item);
            } catch (Exception e) {
                sink.error(e);
            }
        }
    }
}
