package com.exoreaction.xorcery.alchemy.input.file.yaml;

import com.exoreaction.xorcery.alchemy.plugin.InputPlugin;
import com.exoreaction.xorcery.alchemy.plugin.TransmutationConfiguration;
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

@Service(name="plugin.yaml")
public class YamlFileInputPlugin
        implements InputPlugin {

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newInstance(Configuration configuration, TransmutationConfiguration transmutationConfiguration) {
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
