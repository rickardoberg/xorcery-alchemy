package com.exoreaction.xorcery.alchemy.file.csv;

import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.api.ReactiveStreamsContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVReader;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

import java.io.IOException;
import java.util.concurrent.Callable;

class RowReaderStreamer
        implements Subscription {
    private final CSVReader csvReader;
    private final Callable<MetadataJsonNode<JsonNode>> itemReader;
    private final CoreSubscriber<? super MetadataJsonNode<JsonNode>> subscriber;
    private long streamPosition = 0;

    public RowReaderStreamer(CoreSubscriber<? super MetadataJsonNode<JsonNode>> subscriber, CSVReader csvReader, Callable<MetadataJsonNode<JsonNode>> itemReader) {
        this.subscriber = subscriber;
        this.csvReader = csvReader;
        this.itemReader = itemReader;

        // Skip until position
        long skip = new ContextViewElement(subscriber.currentContext())
                .getLong(ReactiveStreamsContext.streamPosition)
                .map(pos -> pos + 1).orElse(0L);
        try {
            csvReader.skip((int) skip);
            streamPosition = skip;
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

    public void request(long request) {
        try {
            if (request == 0)
                return;

            MetadataJsonNode<JsonNode> item = null;
            while (request-- > 0 && (item = itemReader.call()) != null) {
                item.metadata().json().put("timestamp", System.currentTimeMillis());
                item.metadata().json().put("streamPosition", streamPosition++);
                subscriber.onNext(item);
            }

            if (item == null) {
                csvReader.close();
                subscriber.onComplete();
            }
        } catch (Throwable e) {
            try {
                csvReader.close();
            } catch (IOException ex) {
                // Ignore
            }
            subscriber.onError(e);
        }
    }

    @Override
    public void cancel() {
        try {
            csvReader.close();
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }
}
