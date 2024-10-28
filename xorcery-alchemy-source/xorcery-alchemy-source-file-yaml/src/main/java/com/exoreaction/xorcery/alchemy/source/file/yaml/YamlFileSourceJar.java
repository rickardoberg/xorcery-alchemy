package com.exoreaction.xorcery.alchemy.source.file.yaml;

import com.exoreaction.xorcery.alchemy.jar.SourceJar;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Service(name="jars.yaml")
public class YamlFileSourceJar
        implements SourceJar {

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(Configuration configuration, RecipeConfiguration recipeConfiguration) {
        return Flux.from(new YamlPublisher<JsonNode>(JsonNode.class))
                .contextWrite(context ->
                {
                    ContextViewElement contextViewElement = new ContextViewElement(context);
                    if (contextViewElement.has(ResourcePublisherContext.resourceUrl))
                        return context;

                    return configuration.getString("file").map(file ->
                    {
                        URL resourceUrl = Resources.getResource(file).orElseGet(() ->
                        {
                            try {
                                return new File(file).toURI().toURL();
                            } catch (MalformedURLException e) {
                                return null;
                            }
                        });
                        return resourceUrl != null
                                ? context.put(ResourcePublisherContext.resourceUrl, resourceUrl)
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
