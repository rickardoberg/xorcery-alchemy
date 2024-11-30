package dev.xorcery.alchemy.file.excel.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarException;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class ExcelPublisher
        implements Publisher<MetadataJsonNode<JsonNode>> {

    private final JarConfiguration configuration;
    private final RecipeConfiguration recipeConfiguration;
    private JsonNodeFactory instance;

    public ExcelPublisher(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        this.configuration = configuration;
        this.recipeConfiguration = recipeConfiguration;
    }

    @Override
    public void subscribe(Subscriber<? super MetadataJsonNode<JsonNode>> s) {

        if (s instanceof CoreSubscriber<? super MetadataJsonNode<JsonNode>> coreSubscriber) {
            try {
                ContextViewElement contextViewElement = new ContextViewElement(coreSubscriber.currentContext());
                Object resourceUrl = contextViewElement.get(ResourcePublisherContext.resourceUrl)
                        .orElseThrow(ContextViewElement.missing(ResourcePublisherContext.resourceUrl));
                URL excelResource = resourceUrl instanceof URL url ? url : new URL(resourceUrl.toString());

                // Get metadata first
                Metadata.Builder metadataBuilder = new Metadata.Builder();
                try (InputStream resourceIn = excelResource.openStream(); ReadableWorkbook wb = new ReadableWorkbook(resourceIn)) {
                    wb.getSheets().filter(sheet -> sheet.getName().equals("Metadata")).findFirst().ifPresent(metadataSheet ->
                    {
                        try {
                            Iterator<Row> rowIterator = metadataSheet.openStream().iterator();
                            List<String> metadataNames = new ArrayList<>();
                            Row headers = rowIterator.next();
                            headers.forEach(cell -> metadataNames.add(cell.asString()));
                            Iterator<String> nameIterator = metadataNames.iterator();
                            Row metadataValueRow = rowIterator.next();
                            metadataValueRow.forEach(cell -> metadataBuilder.add(nameIterator.next(), toJsonNode(cell)));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
                Metadata metadata = metadataBuilder.build();

                // Stream data
                InputStream resourceIn = excelResource.openStream();
                String dataSheetName = configuration.getString("data").orElse(null);
                ReadableWorkbook wb = new ReadableWorkbook(resourceIn);
                Iterator<Sheet> dataSheets = wb.getSheets().filter(sheet -> !sheet.getName().equals("Metadata") &&
                        (dataSheetName == null || sheet.getName().equals(dataSheetName))).iterator();
                Callable<MetadataJsonNode<JsonNode>> itemReader = new Callable<>() {

                    Sheet sheet;
                    Iterator<Row> rowIterator;
                    final List<String> headerNames = new ArrayList<>();

                    @Override
                    public MetadataJsonNode<JsonNode> call() throws Exception {
                        if (rowIterator == null || !rowIterator.hasNext()) {
                            if (dataSheets.hasNext()) {
                                sheet = dataSheets.next();
                                rowIterator = sheet.read().iterator();
                                if (!rowIterator.hasNext()) {
                                    return null;
                                }

                                Row headers = rowIterator.next();
                                headerNames.clear();
                                headers.forEach(cell -> headerNames.add(cell.asString()));
                                if (!rowIterator.hasNext()) {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        }

                        ObjectNode data = JsonNodeFactory.instance.objectNode();
                        Row row = rowIterator.next();
                        Iterator<String> headerIterator = headerNames.stream().iterator();
                        row.forEach(cell -> data.set(headerIterator.next(), toJsonNode(cell)));
                        return new MetadataJsonNode<>(new Metadata.Builder()
                                .add(metadata)
                                .add("sheet", sheet.getName())
                                .build(), data);
                    }
                };

                coreSubscriber.onSubscribe(new RowReaderStreamer(coreSubscriber, wb, itemReader));
            } catch (Throwable e) {
                coreSubscriber.onError(new JarException(configuration, recipeConfiguration, e));
            }
        } else {
            s.onError(new JarException(configuration, recipeConfiguration, "Subscriber must implement CoreSubscriber"));
        }
    }

    private JsonNode toJsonNode(Cell cell)
    {
        instance = JsonNodeFactory.instance;
        return switch (cell.getType()) {
            case NUMBER -> instance.numberNode(cell.asNumber());
            case STRING -> instance.textNode(cell.asString());
            case BOOLEAN -> instance.booleanNode(cell.asBoolean());
            default -> JsonNodeFactory.instance.missingNode();
        };
    }
}