package contain.opensource.enterprisesearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class IndexingService {

    private final ElasticsearchClient esClient;

    public IndexingService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void indexDocument(EnterpriseSearchDocument doc) {
        try {
            IndexResponse response = esClient.index(i -> i
                .index("enterprise-search-index")
                .id(doc.getId())
                .document(doc)
            );
            
            System.out.println("Document indexed with version " + response.version());
        } catch (Exception e) {
            System.err.println("Failed to index document: " + e.getMessage());
        }
    }

    public void deleteDocument(String id) {
        try {
            DeleteResponse response = esClient.delete(d -> d
                .index("enterprise-search-index")
                .id(id)
            );
            System.out.println("[DELETED] Document removed from index, ID: " + response.id());
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to delete document ID " + id + ": " + e.getMessage());
        }
    }

    public void cleanupDeletedDocumentsForSource(String sourceSystem, Set<String> activeIds) {
    try {
        List<String> indexedIds = getIndexedDocumentIdsBySource(sourceSystem);

        for (String indexedId : indexedIds) {
            if (!activeIds.contains(indexedId)) {
                deleteDocument(indexedId);
            }
        }
    } catch (Exception e) {
        System.err.println("[ERROR] Failed during cleanup of deleted documents: " + e.getMessage());
    }
}

    public List<String> getIndexedDocumentIdsBySource(String sourceSystem) {
    List<String> ids = new ArrayList<>();
    try {
       // SearchResponse<EnterpriseSearchDocument> response = esClient.search(s -> s
       //         .index("enterprise-search-index")
       //         .query(q -> q.term(t -> t.field("source_system").value(sourceSystem))),
       //         EnterpriseSearchDocument.class
       // );

        SearchResponse<EnterpriseSearchDocument> response = esClient.search(s -> s
        .index("enterprise-search-index")
        .query(q -> q.term(t -> t.field("source_system.keyword").value(sourceSystem))),
        EnterpriseSearchDocument.class);

        for (Hit<EnterpriseSearchDocument> hit : response.hits().hits()) {
            if (hit.id() != null) {
                ids.add(hit.id());
            }
        }
    } catch (Exception e) {
        System.err.println("[ERROR] Failed to fetch indexed IDs for source " + sourceSystem + ": " + e.getMessage());
    }
    return ids;
}
}