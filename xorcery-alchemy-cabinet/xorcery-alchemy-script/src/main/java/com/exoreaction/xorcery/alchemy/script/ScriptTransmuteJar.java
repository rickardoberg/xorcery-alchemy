package com.exoreaction.xorcery.alchemy.script;

import com.exoreaction.xorcery.alchemy.jar.JarConfiguration;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.alchemy.jar.TransmuteJar;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.ContextView;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiFunction;

@Service(name = "jars.script")
public class ScriptTransmuteJar
        implements TransmuteJar {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LoggerContext loggerContext;

    @Inject
    public ScriptTransmuteJar(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {

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
                        return new ScriptTransmute(script, out, engine, loggerContext.getLogger(recipeConfiguration.getName()+"."+configuration.getName()));
                    }
            ).orElse((metadataJsonNodeFlux, contextView) -> Flux.error(new IllegalArgumentException("Missing 'script' transformation configuration")));
        } catch (JsonProcessingException e) {
            return (metadataJsonNodeFlux, contextView) -> Flux.error(new IllegalArgumentException("Cannot parse bindings", e));
        }
    }

    static class ScriptTransmute
            implements BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> {
        private final String script;
        private final ByteArrayOutputStream out;
        private final ScriptEngine engine;
        private final Logger logger;

        public ScriptTransmute(String script, ByteArrayOutputStream out, ScriptEngine engine, Logger logger) {
            this.script = script;
            this.out = out;
            this.engine = engine;
            this.logger = logger;
        }

        @Override
        public Publisher<MetadataJsonNode<JsonNode>> apply(Flux<MetadataJsonNode<JsonNode>> flux, ContextView contextView) {
            return flux.handle(this::script);
        }

        private void script(MetadataJsonNode<JsonNode> item, SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
            try {
                Bindings bindings = engine.createBindings();
                bindings.put("metadata", new JsonNodeJSObject(item.metadata().json()));
                bindings.put("data", new JsonNodeJSObject(item.data()));
                engine.eval(script, bindings);

                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }

                sink.next(item);
            } catch (Exception e) {
                sink.error(e);
            }
        }
    }
}
