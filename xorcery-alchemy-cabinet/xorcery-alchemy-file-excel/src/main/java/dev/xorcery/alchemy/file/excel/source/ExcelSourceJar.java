package dev.xorcery.alchemy.file.excel.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import dev.xorcery.util.Resources;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Service(name = "excel")
public class ExcelSourceJar
        implements SourceJar
{
    private final Configuration configuration;

    @Inject
    public ExcelSourceJar(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        return Flux.from(new ExcelPublisher(jarConfiguration, recipeConfiguration))
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
