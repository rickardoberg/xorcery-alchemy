package dev.xorcery.alchemy.watcher;

import dev.xorcery.configuration.Configuration;

import java.io.File;
import java.nio.file.Path;

public record WatcherConfiguration(Configuration configuration) {
    public static WatcherConfiguration get(Configuration configuration) {
        return new WatcherConfiguration(configuration.getConfiguration("watcher"));
    }

    public Path getPath()
    {
        return configuration.getString("path").map(File::new).map(File::toPath).orElseThrow(Configuration.missing("path"));
    }
}
