package contain.opensource.enterprisesearch.services;

import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AlfrescoSyncScheduler {

    private final indexingservice indexingService;

    public AlfrescoSyncScheduler(indexingservice indexingService) {
        this.indexingService = indexingService;
    }

    // Optie A: Draait elke 10 minuten (600.000 milliseconden) nadat de vorige run is voltooid
    @Scheduled(fixedDelay = 600000)
    // Optie B (Alternatief): Elke nacht om 02:00 uur via een cron-expressie
    // @Scheduled(cron = "0 0 2 * * *")
    public void syncChangesFromAlfresco() {
        System.out.println("[INFO] Starting scheduled Alfresco to Elasticsearch sync at " + Instant.now());

        try {
            // 1. Haal gewijzigde documenten op uit Alfresco (bijv. via Alfresco Search/Query API)
            // List<EnterpriseSearchDocument> changedDocs = alfrescoClient.getRecentlyModifiedDocuments();

            // 2. Loop door de documenten en push ze naar Elasticsearch
            /*
            for (EnterpriseSearchDocument doc : changedDocs) {
                indexingService.indexDocument(doc);
            }
            */

            System.out.println("[SUCCESS] Scheduled synchronization completed successfully.");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed during scheduled Alfresco sync: " + e.getMessage());
        }
    }
}