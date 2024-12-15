package dev.xorcery.alchemy.file.csv.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.*;
import dev.xorcery.alchemy.jar.*;
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

    private final JarConfiguration jarConfiguration;
    private final TransmutationConfiguration transmutationConfiguration;

    public CSVPublisher(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {
        this.jarConfiguration = jarConfiguration;
        this.transmutationConfiguration = transmutationConfiguration;
    }

    @Override
    public void subscribe(Subscriber<? super MetadataJsonNode<JsonNode>> s) {

        if (s instanceof CoreSubscriber<? super MetadataJsonNode<JsonNode>> coreSubscriber) {
            try {
                Object sourceUrl = jarConfiguration.get(JarContext.sourceUrl)
                        .orElseThrow(ContextViewElement.missing(JarContext.sourceUrl));
                URL csvResource = sourceUrl instanceof URL url ? url : new URL(sourceUrl.toString());
                String csvResourceUrl = csvResource.toExternalForm();

                CSVParserBuilder parserBuilder = new CSVParserBuilder();

                jarConfiguration.getString("escape").ifPresent(c -> parserBuilder.withEscapeChar(c.charAt(0)));
                jarConfiguration.getString("separator").ifPresent(c -> parserBuilder.withSeparator(c.charAt(0)));
                jarConfiguration.getString("quote").ifPresent(c -> parserBuilder.withQuoteChar(c.charAt(0)));
                CSVParser csvParser = parserBuilder.build();

                if (jarConfiguration.getBoolean("headers").orElse(false)) {
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
                        return new MetadataJsonNode<>(new Metadata.Builder()
                                .add(StandardMetadata.sourceUrl, csvResourceUrl)
                                .build(), data);
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
                        return new MetadataJsonNode<>(new Metadata.Builder()
                                .add(StandardMetadata.sourceUrl, csvResourceUrl)
                                .build(), data);
                    };
                    coreSubscriber.onSubscribe(new RowReaderStreamer(coreSubscriber, csvReader, objectReader));
                }
            } catch (Throwable e) {
                coreSubscriber.onError(new JarException(jarConfiguration, transmutationConfiguration, "CSV parsing failed", e));
            }
        } else {
            s.onError(new JarException(jarConfiguration, transmutationConfiguration, "Subscriber must implement CoreSubscriber"));
        }
    }
}

