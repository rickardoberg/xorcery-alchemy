package com.exoreaction.xorcery.alchemy.output.log;

import com.exoreaction.xorcery.alchemy.plugin.OutputPlugin;
import com.exoreaction.xorcery.alchemy.plugin.TransmutationConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

@Service(name="plugin.log")
public class LogOutputPlugin
    implements OutputPlugin
{
    private final LoggerContext loggerContext;

    @Inject
    public LogOutputPlugin(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newInstance(Configuration configuration, TransmutationConfiguration transmutationConfiguration) {

        Logger logger = loggerContext.getLogger(transmutationConfiguration.getName());
        Level level = org.apache.logging.log4j.Level.toLevel(configuration.getString("level").orElse("info"));
        return (flux,context)->flux.doOnNext(json -> logger.log(level, json.metadata().json().toPrettyString()+":"+(CharSequence) json.data().toPrettyString()));
    }
}
