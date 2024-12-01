package dev.xorcery.alchemy.test;

import dev.xorcery.alchemy.crucible.Crucible;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.opensearch.OpenSearchService;
import dev.xorcery.opensearch.client.search.Document;
import dev.xorcery.opensearch.client.search.SearchQuery;
import dev.xorcery.opensearch.client.search.SearchRequest;
import dev.xorcery.opensearch.client.search.SearchResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Disabled
class OpenSearchTest {

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(b -> b.addResource("testCsvToOpenSearch.yaml"))
            .build();

    @Test
    public void testOpenSearch(Crucible crucible, OpenSearchService openSearchService) throws Exception {
        crucible.getResult().orTimeout(10, TimeUnit.SECONDS).join();

        SearchResponse response = openSearchService.getClient().search().search("numbers", SearchRequest.builder()
                        .query(SearchQuery.match_all())
                .build(), Map.of()).orTimeout(10, TimeUnit.SECONDS).join();

        for (Document document : response.hits().documents()) {
            System.out.println(document.source().toPrettyString());
        }
    }
}