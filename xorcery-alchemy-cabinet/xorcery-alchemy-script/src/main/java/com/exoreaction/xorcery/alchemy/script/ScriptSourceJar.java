package com.exoreaction.xorcery.alchemy.script;

import com.exoreaction.xorcery.alchemy.jar.JarConfiguration;
import com.exoreaction.xorcery.alchemy.jar.JarException;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.alchemy.jar.SourceJar;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import javax.script.*;
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
            return Flux.error(new JarException(configuration, recipeConfiguration, String.format("No script engine named '%s' found", engineName)));
        }
        try {
            Map<String, Object> bindings = mapper.treeToValue(configuration.configuration().getConfiguration("bindings").json(), Map.class);
            engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("bindings", bindings);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            engine.getContext().setWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

            return configuration.getString("script").map(script ->
                    {
                        try {
                            return Flux.generate(new ScriptSource(ScriptExecutor.getScriptExecutor(engine, script),
                                    engine, out, loggerContext.getLogger(recipeConfiguration.getName()+"."+configuration.getName()), configuration, recipeConfiguration));
                        } catch (ScriptException e) {
                            return Flux.<MetadataJsonNode<JsonNode>>error(new JarException(configuration, recipeConfiguration, e));
                        }
                    }
            ).orElseGet(() -> Flux.error(new JarException(configuration, recipeConfiguration, "Missing 'script' transformation configuration")));
        } catch (JsonProcessingException e) {
            return Flux.error(new JarException(configuration, recipeConfiguration, "Cannot parse bindings", e));
        }
    }

    static class ScriptSource
            implements Consumer<SynchronousSink<MetadataJsonNode<JsonNode>>> {
        private final ScriptExecutor script;
        private final ScriptEngine engine;
        private final ByteArrayOutputStream out;
        private final Logger logger;
        private final JarConfiguration configuration;
        private final RecipeConfiguration recipeConfiguration;

        public ScriptSource(ScriptExecutor script, ScriptEngine engine, ByteArrayOutputStream out, Logger logger, JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
            this.script = script;
            this.engine = engine;
            this.out = out;
            this.logger = logger;
            this.configuration = configuration;
            this.recipeConfiguration = recipeConfiguration;
        }

        @Override
        public void accept(SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
            try {
                MetadataJsonNode<JsonNode> item = new MetadataJsonNode<>(new Metadata(JsonNodeFactory.instance.objectNode()), JsonNodeFactory.instance.objectNode());
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
            } catch (Exception e) {
                sink.error(new JarException(configuration, recipeConfiguration, e));
            }
        }
    }

}
