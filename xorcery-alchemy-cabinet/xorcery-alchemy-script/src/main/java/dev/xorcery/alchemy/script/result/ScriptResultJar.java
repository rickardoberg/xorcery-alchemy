package dev.xorcery.alchemy.script.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarException;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.ResultJar;
import dev.xorcery.alchemy.script.ByteArrayOutputStreamWithoutNewLine;
import dev.xorcery.alchemy.script.ScriptFlux;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiFunction;

@Service(name = "script")
public class ScriptResultJar
        implements ResultJar {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LoggerContext loggerContext;

    @Inject
    public ScriptResultJar(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
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
            ByteArrayOutputStream out = new ByteArrayOutputStreamWithoutNewLine();
            engine.getContext().setWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

            return new ScriptFlux(engine, configuration, recipeConfiguration, loggerContext.getLogger(configuration.getName().orElse("script")));
        } catch (JsonProcessingException e) {
            return (metadataJsonNodeFlux, contextView) -> Flux.error(new JarException(configuration, recipeConfiguration, "Cannot parse bindings", e));
        }
    }
}
