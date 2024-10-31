package com.exoreaction.xorcery.alchemy.file.csv;

import com.exoreaction.xorcery.alchemy.jar.JarConfiguration;
import com.exoreaction.xorcery.alchemy.jar.RecipeConfiguration;
import com.exoreaction.xorcery.alchemy.jar.SourceJar;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Service(name = "jars.csv")
public class CSVFileSourceJar
        implements SourceJar {
    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        return Flux.from(new CSVPublisher(configuration, recipeConfiguration))
                .contextWrite(context ->
                {
                    ContextViewElement contextViewElement = new ContextViewElement(context);
                    return contextViewElement.getString(ResourcePublisherContext.resourceUrl).map(url ->
                    {
                        URL resourceUrl = Resources.getResource(url).orElseGet(() ->
                        {
                            try {
                                return new File(url).toURI().toURL();
                            } catch (MalformedURLException e) {
                                return null;
                            }
                        });
                        return resourceUrl != null
                                ? context.put(ResourcePublisherContext.resourceUrl, resourceUrl)
                                : context;

                    }).orElse(context);
                });
    }
}
