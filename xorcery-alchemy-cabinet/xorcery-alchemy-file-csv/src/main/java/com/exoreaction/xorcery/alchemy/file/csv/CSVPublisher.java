package com.exoreaction.xorcery.alchemy.file.csv;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReaderHeaderAware;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

public class CSVPublisher
        implements Publisher<MetadataJsonNode<JsonNode>> {

    public CSVPublisher() {
    }

    @Override
    public void subscribe(Subscriber<? super MetadataJsonNode<JsonNode>> s) {

        if (s instanceof CoreSubscriber<? super MetadataJsonNode<JsonNode>> coreSubscriber) {
            try {
                Object resourceUrl = new ContextViewElement(coreSubscriber.currentContext()).get(ResourcePublisherContext.resourceUrl)
                        .orElseThrow(ContextViewElement.missing(ResourcePublisherContext.resourceUrl));
                URL csvResource = resourceUrl instanceof URL url ? url : new URL(resourceUrl.toString());
                CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(new BufferedReader(new InputStreamReader(csvResource.openStream(), StandardCharsets.UTF_8), 32 * 1024));
                Function<Map<String, String>, MetadataJsonNode<JsonNode>> objectReader = map ->
                {
                    ObjectNode data = JsonNodeFactory.instance.objectNode();
                    map.forEach(data::put);
                    return new MetadataJsonNode<>(new Metadata.Builder().build(), data);
                };
                coreSubscriber.onSubscribe(new RowReaderStreamer(coreSubscriber, csvReader, objectReader));
            } catch (Throwable e) {
                coreSubscriber.onError(e);
            }
        } else {
            s.onError(new IllegalArgumentException("Subscriber must implement CoreSubscriber"));
        }
    }
}

