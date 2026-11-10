package contain.opensource.enterprisesearch.services;

import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Collections;

@Service
public class AlfrescoSyncService {

    private final indexingservice indexingService;
    // Eventueel injecteer je hier een AlfrescoApiClient of WebClient om mee te communiceren

    public AlfrescoSyncService(indexingservice indexingService) {
        this.indexingService = indexingService;
    }

    public void syncChangesFromAlfresco() {
        System.out.println("[INFO] Starting scheduled Alfresco to Elasticsearch sync at " + Instant.now());

        try {
            // 1. Haal gewijzigde documenten op uit Alfresco
            List<EnterpriseSearchDocument> changedDocs = fetchRecentlyModifiedDocumentsFromAlfresco();

            if (changedDocs == null || changedDocs.isEmpty()) {
                System.out.println("[INFO] No new or modified documents found in Alfresco.");
                return;
            }

            // 2. Loop door de documenten en push ze naar Elasticsearch
            for (EnterpriseSearchDocument doc : changedDocs) {
                try {
                    indexingService.indexDocument(doc);
                    System.out.println("[SUCCESS] Indexed document ID: " + doc.getId());
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to index document ID " + doc.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("[INFO] Alfresco sync completed successfully. Total processed: " + changedDocs.size());

        } catch (Exception e) {
            System.err.println("[ERROR] Error during Alfresco synchronization: " + e.getMessage());
        }
    }

    private List<EnterpriseSearchDocument> fetchRecentlyModifiedDocumentsFromAlfresco() {
        // Hier komt de daadwerkelijke HTTP-aanroep naar Alfresco (bijv. via WebClient / RestTemplate)
        // Voor nu een veilige placeholder die je kunt uitbreiden:
        return Collections.emptyList();
    }
}