package dev.xorcery.alchemy.file.excel.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarException;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.ResultJar;
import dev.xorcery.configuration.ApplicationConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.dhatim.fastexcel.VisibilityState;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service(name = "excel")
public class ExcelResultJar
        implements ResultJar {
    private final Configuration configuration;

    @Inject
    public ExcelResultJar(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newResult(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        return (flux, context) -> {
            ContextViewElement contextViewElement = new ContextViewElement(context);
            return contextViewElement.getURI(ResourcePublisherContext.resourceUrl).<Flux<MetadataJsonNode<JsonNode>>>map(resourceUri ->
            {
                try {
                    OutputStream out = resourceUri.getScheme().equals("file")
                            ? new FileOutputStream(new File(resourceUri.getHost()).getAbsoluteFile())
                            : resourceUri.toURL().openConnection().getOutputStream();
                    ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.get(configuration);
                    String version = applicationConfiguration.getVersion();
                    Workbook wb = new Workbook(out, applicationConfiguration.getName(), version.equals("unknown") ? null : version);

                    ExcelState excelState = new ExcelState(new MetadataState(new LinkedHashMap<>()), new LinkedHashMap<>());
                    return flux.<MetadataJsonNode<JsonNode>>map(handleItems(excelState)).doOnTerminate(() -> excelState.write(wb));
                } catch (IOException e) {
                    return Flux.error(new JarException(jarConfiguration, recipeConfiguration, e));
                }
            }).orElseGet(() -> Flux.error(new JarException(jarConfiguration, recipeConfiguration, "No resourceUrl specified")));
        };
    }

    private Function<? super MetadataJsonNode<JsonNode>, ? extends MetadataJsonNode<JsonNode>> handleItems(ExcelState excelState) {
        return item ->
        {
            excelState.apply(item);
            return item;
        };
    }

    record ExcelState(MetadataState metadata, Map<String, SheetState> sheets) {
        public void apply(MetadataJsonNode<JsonNode> item) {
            String sheetName = item.metadata().getString("sheet").orElse("default");
            SheetState sheet = sheets.computeIfAbsent(sheetName, name -> new SheetState(new ArrayList<>(), new ArrayList<>()));
            sheet.apply(item);
            this.metadata.apply(item.metadata());
        }

        public void write(Workbook wb) {
            try {
                metadata.write(wb);
                for (Map.Entry<String, SheetState> sheet : sheets.entrySet()) {
                    Worksheet worksheet = wb.newWorksheet(sheet.getKey());
                    sheet.getValue().write(worksheet);
                    worksheet.close();
                }
                wb.close();
            } catch (IOException e) {
                LogManager.getLogger().warn("Could not close Excel file", e);
            }
        }
    }

    record MetadataState(Map<String, JsonNode> metadata) {
        public void apply(Metadata metadata) {
            metadata.metadata().properties().forEach(entry -> {
                if (!entry.getKey().equals("sheet"))
                {
                    this.metadata.put(entry.getKey(), entry.getValue());
                }
            });
        }

        public void write(Workbook workbook) throws IOException {
            Worksheet worksheet = workbook.newWorksheet("Metadata");
            worksheet.setVisibilityState(VisibilityState.HIDDEN);
            int column = 0;
            for (Map.Entry<String, JsonNode> metadataEntry : metadata.entrySet()) {
                worksheet.value(0, column, metadataEntry.getKey());
                JsonNode value = metadataEntry.getValue();
                writeCell(value, 1, column, value.getNodeType(), worksheet);
            }
        }
    }

    record SheetState(List<ColumnState> columns, List<RowState> rows) {
        public void apply(MetadataJsonNode<JsonNode> item) {
            if (columns.isEmpty()) {
                for (Map.Entry<String, JsonNode> cell : item.data().properties()) {

                    ColumnState columnState = new ColumnState(cell.getKey(), cell.getValue().getNodeType());
                    columns.add(columnState);
                }
            }
            RowState rowState = new RowState(new ArrayList<>());
            rowState.apply(columns, item);
            rows.add(rowState);
        }

        public void write(Worksheet worksheet) {
            for (int i = 0; i < columns.size(); i++) {
                ColumnState column = columns.get(i);
                worksheet.value(0, i, column.header());
            }
            for (int i = 0; i < rows.size(); i++) {
                RowState row = rows.get(i);
                row.write(row, i + 1, columns, worksheet);
            }
        }
    }

    record ColumnState(String header, JsonNodeType type) {
    }

    record RowState(List<JsonNode> cells) {
        public void apply(List<ColumnState> columns, MetadataJsonNode<JsonNode> item) {
            for (ColumnState column : columns) {
                cells.add(item.data().get(column.header()));
            }
        }

        public void write(RowState row, int rowNr, List<ColumnState> columns, Worksheet worksheet) {
            for (int i = 0; i < columns.size(); i++) {
                ColumnState column = columns.get(i);
                JsonNode cell = row.cells().get(i);
                writeCell(cell, rowNr, i, column.type, worksheet);
            }
        }
    }

    private static void writeCell(JsonNode value, int row, int column, JsonNodeType nodeType, Worksheet worksheet)
    {
        switch (nodeType) {
            case ARRAY -> {
            }
            case BINARY -> {
            }
            case BOOLEAN -> worksheet.value(row, column, value.booleanValue());
            case MISSING -> {
            }
            case NULL -> {
            }
            case NUMBER -> worksheet.value(row, column, value.numberValue());
            case OBJECT -> {
            }
            case POJO -> {
            }
            case STRING -> worksheet.value(row, column, value.textValue());
        }
    }
}
