package dev.xorcery.alchemy.file.csv.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.*;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarContext;
import dev.xorcery.alchemy.jar.JarException;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;

public class CSVPublisher
        implements Publisher<MetadataJsonNode<JsonNode>> {

    private final JarConfiguration configuration;
    private final RecipeConfiguration recipeConfiguration;

    public CSVPublisher(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        this.configuration = configuration;
        this.recipeConfiguration = recipeConfiguration;
    }

    @Override
    public void subscribe(Subscriber<? super MetadataJsonNode<JsonNode>> s) {

        if (s instanceof CoreSubscriber<? super MetadataJsonNode<JsonNode>> coreSubscriber) {
            try {
                ContextViewElement contextViewElement = new ContextViewElement(coreSubscriber.currentContext());
                Object sourceUrl = contextViewElement.get(JarContext.sourceUrl)
                        .orElseThrow(ContextViewElement.missing(JarContext.sourceUrl));
                URL csvResource = sourceUrl instanceof URL url ? url : new URL(sourceUrl.toString());

                CSVParserBuilder parserBuilder = new CSVParserBuilder();
                contextViewElement.getString("escape").ifPresent(c -> parserBuilder.withEscapeChar(c.charAt(0)));
                contextViewElement.getString("separator").ifPresent(c -> parserBuilder.withSeparator(c.charAt(0)));
                contextViewElement.getString("quote").ifPresent(c -> parserBuilder.withQuoteChar(c.charAt(0)));
                CSVParser csvParser = parserBuilder.build();

                if (contextViewElement.getBoolean("headers").orElse(false)) {
                    CSVReaderHeaderAware csvReader = new CSVReaderHeaderAwareBuilder(new BufferedReader(new InputStreamReader(csvResource.openStream(), StandardCharsets.UTF_8)))
                            .withCSVParser(csvParser)
                            .build();
                    Callable<MetadataJsonNode<JsonNode>> itemReader = () ->
                    {
                        Map<String, String> map = csvReader.readMap();
                        if (map == null)
                            return null;
                        ObjectNode data = JsonNodeFactory.instance.objectNode();
                        map.forEach(data::put);
                        return new MetadataJsonNode<>(new Metadata.Builder().build(), data);
                    };
                    coreSubscriber.onSubscribe(new RowReaderStreamer(coreSubscriber, csvReader, itemReader));
                } else {
                    CSVReader csvReader = new CSVReaderBuilder(new BufferedReader(new InputStreamReader(csvResource.openStream(), StandardCharsets.UTF_8)))
                            .withCSVParser(csvParser)
                            .build();
                    Callable<MetadataJsonNode<JsonNode>> objectReader = () ->
                    {
                        String[] row = csvReader.readNext();
                        if (row == null)
                            return null;
                        ArrayNode data = JsonNodeFactory.instance.arrayNode();
                        for (String column : row) {
                            data.add(column);
                        }
                        return new MetadataJsonNode<>(new Metadata.Builder().build(), data);
                    };
                    coreSubscriber.onSubscribe(new RowReaderStreamer(coreSubscriber, csvReader, objectReader));
                }
            } catch (Throwable e) {
                coreSubscriber.onError(new JarException(configuration, recipeConfiguration, e));
            }
        } else {
            s.onError(new JarException(configuration, recipeConfiguration, "Subscriber must implement CoreSubscriber"));
        }
    }
}

