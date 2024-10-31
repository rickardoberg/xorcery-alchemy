package com.exoreaction.xorcery.alchemy.script;

import com.exoreaction.xorcery.alchemy.jar.JarConfiguration;
import com.exoreaction.xorcery.alchemy.jar.JarException;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.alchemy.jar.ResultJar;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.ContextView;

import javax.script.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiFunction;

@Service(name = "jars.script")
public class ScriptResultJar
        implements ResultJar {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger;

    @Inject
    public ScriptResultJar(Logger logger) {
        this.logger = logger;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newResult(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        String engineName = configuration.getString("engine").orElse("nashorn");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName(engineName);
        if (engine == null) {
            return (m, c) -> Flux.error(new IllegalArgumentException(String.format("No script engine named '%s' found", engineName)));
        }
        try {
            Map<String, Object> bindings = mapper.treeToValue(configuration.configuration().getConfiguration("bindings").json(), Map.class);
            engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("bindings", bindings);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            engine.getContext().setWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

            return configuration.getString("script").<BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>>>map(script ->
                    {
                        try {
                            return new ScriptResult(ScriptExecutor.getScriptExecutor(engine, script), out, engine, configuration, recipeConfiguration);
                        } catch (ScriptException e) {
                            return (metadataJsonNodeFlux, contextView) -> Flux.error(new JarException(configuration, recipeConfiguration, e));
                        }
                    }
            ).orElse((metadataJsonNodeFlux, contextView) -> Flux.error(new JarException(configuration, recipeConfiguration, "Missing 'jslt' transformation configuration")));
        } catch (JsonProcessingException e) {
            return (metadataJsonNodeFlux, contextView) -> Flux.error(new JarException(configuration, recipeConfiguration, "Cannot parse bindings", e));
        }
    }

    class ScriptResult
            implements BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> {
        private final ScriptExecutor script;
        private final ByteArrayOutputStream out;
        private final ScriptEngine engine;
        private final JarConfiguration configuration;
        private final RecipeConfiguration recipeConfiguration;

        public ScriptResult(ScriptExecutor script, ByteArrayOutputStream out, ScriptEngine engine, JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
            this.script = script;
            this.out = out;
            this.engine = engine;
            this.configuration = configuration;
            this.recipeConfiguration = recipeConfiguration;
        }

        @Override
        public Publisher<MetadataJsonNode<JsonNode>> apply(Flux<MetadataJsonNode<JsonNode>> flux, ContextView contextView) {
            return flux.handle(this::script);
        }

        private void script(MetadataJsonNode<JsonNode> item, SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
            try {
                Bindings bindings = engine.createBindings();
                ObjectNode itemJson = JsonNodeFactory.instance.objectNode();
                itemJson.set("metadata", item.metadata().json());
                itemJson.set("data", item.data());
                bindings.put("item", new JsonNodeJSObject(itemJson));
                bindings.put("sink", new JavaScriptSink(sink));
                script.call(bindings);

                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }

                sink.next(item);
            } catch (Throwable e) {
                sink.error(new JarException(configuration, recipeConfiguration, e));
            }
        }
    }
}
