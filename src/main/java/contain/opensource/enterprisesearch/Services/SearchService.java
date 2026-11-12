package contain.opensource.enterprisesearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator; // Vergeet niet deze import toe te voegen bovenaan

@Service
public class SearchService {

    private final ElasticsearchClient esClient;

    public SearchService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public List<EnterpriseSearchDocument> searchDocuments(String query) {
        List<EnterpriseSearchDocument> results = new ArrayList<>();
        try {
            SearchResponse<EnterpriseSearchDocument> response;

            if (query == null || query.trim().isEmpty()) {
                response = esClient.search(s -> s
                        .index("enterprise-search-index")
                        .size(100) // Zorg dat de limiet op 100 staat
                        .query(q -> q.matchAll(m -> m)),
                        EnterpriseSearchDocument.class);
            } else {
                String wildcardQuery = "*" + query.toLowerCase() + "*";
                
                response = esClient.search(s -> s
                        .index("enterprise-search-index")
                        .size(100) // Zorg dat de limiet op 100 staat
                        .query(q -> q.bool(b -> b
                                .should(sh -> sh.multiMatch(m -> m
                                        .fields("title", "content", "classification")
                                        .query(query)
                                ))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("title")
                                        .value(wildcardQuery)
                                ))
                        )),
                        EnterpriseSearchDocument.class);
            }

            for (Hit<EnterpriseSearchDocument> hit : response.hits().hits()) {
                EnterpriseSearchDocument doc = hit.source();
                if (doc != null) {
                    if (doc.getId() == null) {
                        doc.setId(hit.id());
                    }
                    results.add(doc);
                }
            }

            // Alfabetisch sorteren op filenaam (title) (hoofdletterongevoelig)
            results.sort(Comparator.comparing(
                doc -> doc.getTitle() != null ? doc.getTitle().toLowerCase() : ""
            ));

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to execute search query: " + e.getMessage());
        }
        return results;
    }
}