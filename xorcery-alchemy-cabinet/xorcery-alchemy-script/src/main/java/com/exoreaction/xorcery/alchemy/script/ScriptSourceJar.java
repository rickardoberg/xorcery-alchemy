package com.exoreaction.xorcery.alchemy.script;

import com.exoreaction.xorcery.alchemy.jar.JarConfiguration;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.alchemy.jar.SourceJar;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

@Service(name = "jars.script")
public class ScriptSourceJar
        implements SourceJar {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LoggerContext loggerContext;

    @Inject
    public ScriptSourceJar(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        String engineName = configuration.getString("engine").orElse("nashorn");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName(engineName);
        if (engine == null) {
            return Flux.error(new IllegalArgumentException(String.format("No script engine named '%s' found", engineName)));
        }
        try {
            Map<String, Object> bindings = mapper.treeToValue(configuration.configuration().getConfiguration("bindings").json(), Map.class);
            engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("bindings", bindings);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            engine.getContext().setWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

            return configuration.getString("script").map(script ->
                    Flux.generate(new ScriptSource(script, out, engine, loggerContext.getLogger(recipeConfiguration.getName()+"."+configuration.getName())))
            ).orElseGet(() -> Flux.error(new IllegalArgumentException("Missing 'script' transformation configuration")));
        } catch (JsonProcessingException e) {
            return Flux.error(new IllegalArgumentException("Cannot parse bindings", e));
        }
    }

    class ScriptSource
            implements Consumer<SynchronousSink<MetadataJsonNode<JsonNode>>> {
        private final String script;
        private final ByteArrayOutputStream out;
        private final ScriptEngine engine;
        private final Logger logger;

        public ScriptSource(String script, ByteArrayOutputStream out, ScriptEngine engine, Logger logger) {
            this.script = script;
            this.out = out;
            this.engine = engine;
            this.logger = logger;
        }

        @Override
        public void accept(SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
            try {
                MetadataJsonNode<JsonNode> item = new MetadataJsonNode<>(new Metadata(JsonNodeFactory.instance.objectNode()), JsonNodeFactory.instance.objectNode());
                Bindings bindings = engine.createBindings();
                bindings.put("metadata", new JsonNodeJSObject(item.metadata().json()));
                bindings.put("data", new JsonNodeJSObject(item.data()));
                engine.eval(script, bindings);

                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }

                if (bindings.getOrDefault("data", null) != null)
                    sink.next(item);
                else
                    sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        }
    }
}
