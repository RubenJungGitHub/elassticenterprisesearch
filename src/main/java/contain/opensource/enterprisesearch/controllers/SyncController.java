package contain.opensource.enterprisesearch.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.enterprisesearch.services.AlfrescoSyncService;

@RestController
@RequestMapping("/api/search")
public class SyncController {

    private final AlfrescoSyncService syncService;

    public SyncController(AlfrescoSyncService syncService) {
        this.syncService = syncService;
    }

    // 1. Het REST-endpoint om de sync handmatig te "kicken"
    @PostMapping("/sync-alfresco")
    public ResponseEntity<String> triggerManualSync() {
        try {
            // Roep dezelfde logica aan
            syncService.syncChangesFromAlfresco();
            return ResponseEntity.ok("[SUCCESS] Manual Alfresco synchronization triggered and completed.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("[ERROR] Sync failed: " + e.getMessage());
        }
    }

    // 2. De scheduler die exact hetzelfde endpoint/methode aanroept op een vaste interval (bijv. elke 10 minuten)
    @Scheduled(fixedDelay = 600000)
    public void triggerScheduledSync() {
        System.out.println("[INFO] Scheduled sync triggered automatically.");
        try {
            syncService.syncChangesFromAlfresco();
        } catch (Exception e) {
            System.err.println("[ERROR] Scheduled sync failed: " + e.getMessage());
        }
    }
}