package dev.xorcery.alchemy.file.yaml.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarContext;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import dev.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import dev.xorcery.util.Resources;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Service(name="yaml")
public class YamlFileSourceJar
        implements SourceJar {

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        return Flux.from(new YamlPublisher<JsonNode>(JsonNode.class))
                .contextCapture()
                .contextWrite(context ->
                {
                    ContextViewElement contextViewElement = new ContextViewElement(context);
                    return contextViewElement.getString(JarContext.sourceUrl).map(url ->
                    {
                        URL sourceUrl = Resources.getResource(url).orElseGet(() ->
                        {
                            try {
                                return new File(url).toURI().toURL();
                            } catch (MalformedURLException e) {
                                return null;
                            }
                        });
                        return sourceUrl != null
                                ? context.put(ResourcePublisherContext.resourceUrl, sourceUrl)
                                : context;
                    }).orElse(context);
                }).map(json ->
                {
                    ObjectNode metadata = JsonNodeFactory.instance.objectNode();
                    metadata.set("timestamp", JsonNodeFactory.instance.numberNode(System.currentTimeMillis()));
                    return new MetadataJsonNode<>(new Metadata(metadata), json);
                });
    }
}
