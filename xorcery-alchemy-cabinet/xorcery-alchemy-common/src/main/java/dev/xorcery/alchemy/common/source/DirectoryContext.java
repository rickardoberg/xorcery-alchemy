package dev.xorcery.alchemy.common.source;

import dev.xorcery.collections.MapElement;

import java.util.Map;

import static dev.xorcery.configuration.Configuration.missing;

public record DirectoryContext(Map<String, Object> map)
    implements MapElement<String, Object>
{
    public String getPath() {
        return getString("path").orElseThrow(missing("path"));
    }

    public String getFilter() {
        return getString("filter").orElse("*.*");
    }
}
