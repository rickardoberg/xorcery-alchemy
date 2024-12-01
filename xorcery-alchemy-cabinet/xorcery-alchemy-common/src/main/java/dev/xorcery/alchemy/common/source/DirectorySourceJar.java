package dev.xorcery.alchemy.common.source;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.Cabinet;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.SourceJar;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static dev.xorcery.configuration.Configuration.missing;

@Service(name = "directory")
public class DirectorySourceJar
        implements SourceJar {

    private final Cabinet cabinet;

    @Inject
    public DirectorySourceJar(Cabinet cabinet) {
        this.cabinet = cabinet;
    }

    @Override
    public Flux<MetadataJsonNode<JsonNode>> newSource(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration) {
        try {
            Path directory = Path.of(jarConfiguration.getString("path").orElseThrow(missing("path")));
            String matcherPattern = jarConfiguration.getString("filter").orElse("*.*");

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

            return Flux.concat(Flux.fromIterable(filePaths).handle(this::publishFile));
        } catch (Throwable e) {
            return Flux.error(e);
        }
    }

    private void publishFile(Path path, SynchronousSink<Publisher<? extends MetadataJsonNode<JsonNode>>> publisherSynchronousSink) {

    }
}
