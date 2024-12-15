package dev.xorcery.alchemy.common.source;

import dev.xorcery.alchemy.jar.JarConfiguration;

import static dev.xorcery.configuration.Configuration.missing;

public record DirectoryConfiguration(JarConfiguration configuration)
{
    public String getPath() {
        return configuration.getString("path").orElseThrow(missing("path"));
    }

    public String getFilter() {
        return configuration.getString("filter").orElse("*.*");
    }
}
