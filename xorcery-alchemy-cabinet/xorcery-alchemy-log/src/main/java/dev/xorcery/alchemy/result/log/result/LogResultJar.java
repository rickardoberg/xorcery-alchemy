package dev.xorcery.alchemy.result.log.result;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.ResultJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

@Service(name="log")
public class LogResultJar
    implements ResultJar
{
    private final Configuration configuration;
    private final LoggerContext loggerContext;

    @Inject
    public LogResultJar(Configuration configuration, LoggerContext loggerContext) {
        this.configuration = configuration;
        this.loggerContext = loggerContext;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newResult(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        Logger logger = loggerContext.getLogger(jarConfiguration.getName().or(recipeConfiguration::getName).or(()->configuration.getString("jars.log.result.logger")).orElse("log"));
        Level level = Level.toLevel(jarConfiguration.getString("level").orElseGet(()->configuration.getString("jars.log.result.level").orElse("info")));

        return (flux,context)->flux.doOnNext(json -> logger.log(level, toMessage(json)));
    }

    private String toMessage(MetadataJsonNode<JsonNode> json) {
        return (!json.metadata().json().isEmpty() ? json.metadata().json().toPrettyString()+":":"")+(!json.data().isEmpty() ? json.data().toPrettyString():"");
    }
}
