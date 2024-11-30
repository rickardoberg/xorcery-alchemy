package dev.xorcery.alchemy.watcher;


import dev.xorcery.alchemy.crucible.Crucible;
import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service(name = "watcher")
@RunLevel(20)
public class Watcher
    implements PreDestroy
{

    private final Crucible crucible;
    private final Logger logger;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final WatchService watchService;
    private Path directoryPath;

    @Inject
    public Watcher(Crucible crucible, Configuration configuration, Logger logger) throws IOException {
        this(crucible, WatcherConfiguration.get(configuration), logger);
    }

    public Watcher(Crucible crucible, WatcherConfiguration watcherConfiguration, Logger logger) throws IOException {
        this.crucible = crucible;
        this.logger = logger;

        FileSystem fileSystem = FileSystems.getDefault();
        watchService = fileSystem.newWatchService();
        directoryPath = watcherConfiguration.getPath();
        WatchKey watchKey = directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        executorService.submit(this::watchDirectory);
    }

    private void watchDirectory() {
        try {
            WatchKey key = watchService.poll(10, TimeUnit.SECONDS);

            for (WatchEvent<?> pollEvent : key.pollEvents()) {
                if (pollEvent.context() instanceof Path pathToFile)
                {
                }
            }

        } catch (InterruptedException e) {
        }
        executorService.submit(this::watchDirectory);
    }

    @Override
    public void preDestroy() {
        try {
            watchService.close();
            executorService.shutdown();
            executorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (Throwable e) {
            logger.warn("Could not shutdown Watcher", e);
        }
    }
}
