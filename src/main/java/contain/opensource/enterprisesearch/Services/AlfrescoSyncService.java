package contain.opensource.enterprisesearch.services;

import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import org.springframework.stereotype.Service;

@Service
public class AlfrescoSyncService {

    private final indexingservice indexingService;

    public AlfrescoSyncService(indexingservice indexingService) {
        this.indexingService = indexingService;
    }

    public void executeSync() {
        System.out.println("[INFO] Executing Alfresco synchronization workflow...");
        // Hier komt straks de logica om documenten op te halen uit Alfresco
        // en via indexingService.indexDocument(...) weg te schrijven naar Elasticsearch.
    }
}