package dev.xorcery.alchemy.domainevents;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record GraphQLConfiguration(Configuration configuration) {

    public static GraphQLConfiguration get(Configuration configuration)
    {
        return new GraphQLConfiguration(configuration.getConfiguration("graphql"));
    }

    public List<String> getSchemas()
    {
        return configuration.getListAs("schemas", JsonNode::asText).orElseGet(Collections::emptyList);
    }
}
