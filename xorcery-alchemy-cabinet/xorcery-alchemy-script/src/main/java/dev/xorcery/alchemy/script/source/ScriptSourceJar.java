package dev.xorcery.alchemy.script.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarException;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.alchemy.script.ByteArrayOutputStreamWithoutNewLine;
import dev.xorcery.alchemy.script.JavaScriptSynchronousSink;
import dev.xorcery.alchemy.script.JsonNodeJSObject;
import dev.xorcery.alchemy.script.ScriptExecutor;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

@Service(name = "script", metadata = "enabled=jars.enabled")
public class ScriptSourceJar
        implements SourceJar {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LoggerContext loggerContext;

    @Inject
    public ScriptSourceJar(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        String engineName = jarConfiguration.getString("engine").orElse("nashorn");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName(engineName);
        if (engine == null) {
            return Flux.error(new JarException(jarConfiguration, recipeConfiguration, String.format("No script engine named '%s' found", engineName)));
        }

        try {
            Map<String, Object> bindings = mapper.treeToValue(jarConfiguration.configuration().getConfiguration("bindings").json(), Map.class);
            engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("bindings", bindings);
            ByteArrayOutputStream out = new ByteArrayOutputStreamWithoutNewLine();
            engine.getContext().setWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            ScriptSource scriptSource = new ScriptSource(engine, jarConfiguration, recipeConfiguration, out, loggerContext.getLogger(jarConfiguration.getName().orElse("script")));
            return Flux.generate(scriptSource, scriptSource).publishOn(Schedulers.boundedElastic());
        } catch (JsonProcessingException e) {
            return Flux.error(new JarException(jarConfiguration, recipeConfiguration, "Cannot parse bindings", e));
        }
    }

    static class ScriptSource
            implements
            Callable<Bindings>,
            BiFunction<Bindings, SynchronousSink<MetadataJsonNode<JsonNode>>, Bindings> {
        private final ByteArrayOutputStream out;
        private final Logger logger;
        private final ScriptExecutor subscribe;
        private final ScriptExecutor next;

        private final ScriptEngine engine;
        private final JarConfiguration configuration;
        private final RecipeConfiguration recipeConfiguration;

        public ScriptSource(ScriptEngine engine, JarConfiguration configuration, RecipeConfiguration recipeConfiguration, ByteArrayOutputStream out, Logger logger) {
            this.out = out;
            this.logger = logger;
            this.subscribe = configuration.getString("subscribe").map(script -> ScriptExecutor.getScriptExecutor(engine, script)).orElse(null);
            this.next = configuration.getString("next").map(script -> ScriptExecutor.getScriptExecutor(engine, script)).orElse(null);
            this.engine = engine;
            engine.getContext().getWriter();
            this.configuration = configuration;
            this.recipeConfiguration = recipeConfiguration;
        }

        @Override
        public Bindings call() throws Exception {
            Bindings bindings = engine.createBindings();
            bindings.put("metadata", new JsonNodeJSObject(JsonNodeFactory.instance.objectNode()));
            if (subscribe != null)
            {
                subscribe.call(bindings);
                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }
            }
            return bindings;
        }

        @Override
        public Bindings apply(Bindings bindings, SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
            try {
                MetadataJsonNode<JsonNode> item = new MetadataJsonNode<>(new Metadata(JsonNodeFactory.instance.objectNode()), ((JsonNodeJSObject)bindings.get("metadata")).getJsonNode().deepCopy());
                ObjectNode itemJson = JsonNodeFactory.instance.objectNode();
                itemJson.set("metadata", item.metadata().json());
                itemJson.set("data", item.data());
                bindings.put("item", new JsonNodeJSObject(itemJson));
                bindings.put("sink", new JavaScriptSynchronousSink(sink));
                next.call(bindings);

                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }
            } catch (Exception e) {
                sink.error(new JarException(configuration, recipeConfiguration, e));
            }
            return bindings;
        }
    }
}
