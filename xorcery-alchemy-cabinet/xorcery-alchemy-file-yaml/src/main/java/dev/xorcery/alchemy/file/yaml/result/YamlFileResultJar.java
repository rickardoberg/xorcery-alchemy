package dev.xorcery.alchemy.file.yaml.result;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.alchemy.jar.*;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.function.BiFunction;

@Service(name = "yaml")
public class YamlFileResultJar
        implements ResultJar {

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newResult(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        return (flux, context) ->
        {
            ContextViewElement contextViewElement = new ContextViewElement(context);
            URI fileUrl = contextViewElement.getURI(JarContext.resultUrl).orElse(null);

            if (fileUrl == null) {
                return Flux.error(new JarException(configuration, recipeConfiguration, "Could not find file"));
            }

            if (fileUrl.getScheme().equals("file")) {
                new File(fileUrl.toASCIIString().substring("file://".length())).getParentFile().mkdirs();
            }

            try {
                BufferedOutputStream outputStream = new BufferedOutputStream(fileUrl.getScheme().equals("file")
                        ? new FileOutputStream(new File(fileUrl.toASCIIString().substring("file://".length())).getAbsoluteFile())
                        : fileUrl.toURL().openConnection().getOutputStream());
                ObjectMapper mapper = new YAMLMapper().findAndRegisterModules()
                        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .enable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
                        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
                return flux.doOnTerminate(() ->
                {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }).handle((item, sink) ->
                {
                    try {
                        mapper.writeValue(outputStream, item);
                        sink.next(item);
                    } catch (IOException e) {
                        sink.error(e);
                    }
                });
            } catch (Throwable e) {
                return Flux.error(new JarException(configuration, recipeConfiguration, e));
            }
        };
    }
}
