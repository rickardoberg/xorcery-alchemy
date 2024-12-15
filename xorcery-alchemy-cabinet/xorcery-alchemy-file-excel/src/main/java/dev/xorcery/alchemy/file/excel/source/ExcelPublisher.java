package dev.xorcery.alchemy.file.excel.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.*;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.dhatim.fastexcel.reader.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class ExcelPublisher
        implements Publisher<MetadataJsonNode<JsonNode>> {

    private final JarConfiguration jarConfiguration;
    private final TransmutationConfiguration transmutationConfiguration;
    private JsonNodeFactory instance;

    public ExcelPublisher(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration) {
        this.jarConfiguration = jarConfiguration;
        this.transmutationConfiguration = transmutationConfiguration;
    }

    @Override
    public void subscribe(Subscriber<? super MetadataJsonNode<JsonNode>> s) {

        if (s instanceof CoreSubscriber<? super MetadataJsonNode<JsonNode>> coreSubscriber) {
            try {
                Object sourceUrl = jarConfiguration.get(JarContext.sourceUrl)
                        .orElseThrow(Configuration.missing(JarContext.sourceUrl.name()));
                URL excelResource = sourceUrl instanceof URL url ? url : new URL(sourceUrl.toString());

                // Get metadata first
                Metadata.Builder metadataBuilder = new Metadata.Builder();
                metadataBuilder.add(StandardMetadata.sourceUrl, excelResource.toExternalForm());
                try (InputStream resourceIn = excelResource.openStream(); ReadableWorkbook wb = new ReadableWorkbook(resourceIn, new ReadingOptions(true, false))) {
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
                String dataSheetName = jarConfiguration.getString("data").orElse(null);
                ReadableWorkbook wb = new ReadableWorkbook(resourceIn, new ReadingOptions(true, false));
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
                                headers.forEach(cell -> headerNames.add(hasDateFormat(cell)
                                        ? cell.asDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                        : cell.asString()));
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
                coreSubscriber.onError(new JarException(jarConfiguration, transmutationConfiguration, "Excel parsing failed", e));
            }
        } else {
            s.onError(new JarException(jarConfiguration, transmutationConfiguration, "Subscriber must implement CoreSubscriber"));
        }
    }

    private JsonNode toJsonNode(Cell cell) {
        instance = JsonNodeFactory.instance;
        return switch (cell.getType()) {
            case NUMBER -> hasDateFormat(cell)
                    ? instance.textNode(cell.asDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    : instance.numberNode(cell.asNumber());
            case STRING -> instance.textNode(cell.asString());
            case BOOLEAN -> instance.booleanNode(cell.asBoolean());
            default -> JsonNodeFactory.instance.missingNode();
        };
    }

    private boolean hasDateFormat(Cell cell) {
        String dataFormatString = cell.getDataFormatString();
        return dataFormatString != null && (dataFormatString.contains("y") || dataFormatString.contains("m") || dataFormatString.contains("d"));
    }
}