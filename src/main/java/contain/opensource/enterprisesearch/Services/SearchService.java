
package contain.opensource.enterprisesearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final ElasticsearchClient esClient;

    public SearchService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public List<EnterpriseSearchDocument> searchDocuments(String query) {
        List<EnterpriseSearchDocument> results = new ArrayList<>();
        try {
            // Als de query leeg is, tonen we standaard alles (of een lege lijst, naar
            // keuze)
            SearchResponse<EnterpriseSearchDocument> response;

            if (query == null || query.trim().isEmpty()) {
                response = esClient.search(s -> s
                        .index("enterprise-search-index")
                        .query(q -> q.matchAll(m -> m)), // <--- Aangepast: m -> m meegegeven
                        EnterpriseSearchDocument.class);
            } else {
                // Zoek op zowel titel als content
                response = esClient.search(s -> s
                        .index("enterprise-search-index")
                        .query(q -> q.multiMatch(m -> m
                                .fields("title", "content")
                                .query(query))),
                        EnterpriseSearchDocument.class);
            }

            for (Hit<EnterpriseSearchDocument> hit : response.hits().hits()) {
                EnterpriseSearchDocument doc = hit.source();
                if (doc != null) {
                    // Zet eventueel het Elasticsearch ID als fallback als ID leeg is
                    if (doc.getId() == null) {
                        doc.setId(hit.id());
                    }
                    results.add(doc);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to execute search query: " + e.getMessage());
        }
        return results;
    }
}