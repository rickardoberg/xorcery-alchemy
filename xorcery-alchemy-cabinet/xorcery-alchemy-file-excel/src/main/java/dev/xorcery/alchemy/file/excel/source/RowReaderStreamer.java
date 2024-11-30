package dev.xorcery.alchemy.file.excel.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

import java.io.IOException;
import java.util.concurrent.Callable;

public class RowReaderStreamer
        implements Subscription {
    private final ReadableWorkbook workBook;
    private final Callable<MetadataJsonNode<JsonNode>> itemReader;
    private final CoreSubscriber<? super MetadataJsonNode<JsonNode>> subscriber;
    private long streamPosition = 0;

    public RowReaderStreamer(CoreSubscriber<? super MetadataJsonNode<JsonNode>> subscriber, ReadableWorkbook workBook, Callable<MetadataJsonNode<JsonNode>> itemReader) {
        this.subscriber = subscriber;
        this.workBook = workBook;
        this.itemReader = itemReader;

        // Skip until position
        long skip = new ContextViewElement(subscriber.currentContext())
                .getLong(ReactiveStreamsContext.streamPosition)
                .map(pos -> pos + 1).orElse(0L);
        try {
            for (int i = 0; i < skip; i++) {
                itemReader.call();
            }
            streamPosition = skip;
        } catch (Throwable e) {
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
                workBook.close();
                subscriber.onComplete();
            }
        } catch (Throwable e) {
            try {
                workBook.close();
            } catch (IOException ex) {
                // Ignore
            }
            subscriber.onError(e);
        }
    }

    @Override
    public void cancel() {
        try {
            workBook.close();
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

}
