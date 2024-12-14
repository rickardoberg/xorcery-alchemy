package dev.xorcery.alchemy.common.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static dev.xorcery.alchemy.jar.StandardMetadata.timestamp;

@Service(name = "directory", metadata = "enabled=jars.enabled")
public class DirectorySourceJar
        implements SourceJar {

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        try {
            DirectoryContext directoryContext = new DirectoryContext(jarConfiguration.getContext());
            Path directory = Path.of(directoryContext.getPath());

            String matcherPattern = directoryContext.getFilter();

            final List<Path> filePaths = new ArrayList<>();
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(matcherPattern);
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matcher.matches(file)) {
                        filePaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            return Flux.fromIterable(filePaths).handle(this::publishFile);
        } catch (Throwable e) {
            return Flux.error(e);
        }
    }

    private void publishFile(Path path, SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
        try {
            MetadataJsonNode<JsonNode> item = new MetadataJsonNode<>(
                    new Metadata.Builder()
                            .add(timestamp, path.toFile().lastModified())
                            .build(),
                    JsonNodeFactory.instance.objectNode().put("resourceUrl", path.toUri().toURL().toExternalForm()));
            sink.next(item);
        } catch (Throwable e) {
            sink.error(e);
        }
    }
}
