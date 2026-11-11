package contain.opensource.enterprisesearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import co.elastic.clients.elasticsearch.core.IndexResponse;

import org.springframework.stereotype.Service;

@Service
public class IndexingService
    {
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
}