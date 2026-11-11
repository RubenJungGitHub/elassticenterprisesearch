package contain.opensource.enterprisesearch.services;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AlfrescoSyncScheduler {
    final String ANSI_GREEN_BG = "\u001B[42m"; // Groene achtergrond
    // Rood
    final String ANSI_RED        = "\u001B[31m"; // Rode tekst
    final String ANSI_RED_BG     = "\u001B[41m"; // Rode achtergrond

    // Geel
    final String ANSI_YELLOW     = "\u001B[33m"; // Gele tekst
    final String ANSI_YELLOW_BG  = "\u001B[43m"; // Gele achtergrond

    // Cyaan (lichtblauw/groenachtig)
    final String ANSI_CYAN       = "\u001B[36m"; // Cyaan tekst
    final String ANSI_CYAN_BG    = "\u001B[46m"; // Cyaan achtergrond
    final String ANSI_BLACK    = "\u001B[30m"; // Zwarte tekst (voor goede leesbaarheid)
    final String ANSI_RESET    = "\u001B[0m";  // Reset naar standaard
    private final AlfrescoSyncService alfrescoSyncService;

    public AlfrescoSyncScheduler(AlfrescoSyncService alfrescoSyncService) {
        this.alfrescoSyncService = alfrescoSyncService;
    }        

    // Optie A: Draait elke 10 minuten (600.000 milliseconden) nadat de vorige run is voltooid

    // Optie B (Alternatief): Elke nacht om 02:00 uur via een cron-expressie
    // @Scheduled(cron = "0 0 2 * * *")
    @Scheduled(fixedDelay = 10000)
    public void syncChangesFromAlfresco() {
        // Printen met groene achtergrond
        System.out.println(ANSI_GREEN_BG + ANSI_BLACK + Instant.now() + " [INFO] Starting scheduled Alfresco to Elasticsearch sync" + ANSI_RESET);
        alfrescoSyncService.syncChangesFromAlfresco();        

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