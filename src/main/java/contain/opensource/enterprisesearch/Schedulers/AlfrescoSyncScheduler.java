package contain.opensource.enterprisesearch.Schedulers;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import contain.opensource.enterprisesearch.services.AlfrescoSyncService;
import static contain.opensource.enterprisesearch.util.AnsiColor.*;

import java.time.Instant;

@Component
public class AlfrescoSyncScheduler {

    private final AlfrescoSyncService alfrescoSyncService;

    public AlfrescoSyncScheduler(AlfrescoSyncService alfrescoSyncService) {
        this.alfrescoSyncService = alfrescoSyncService;
    }

    @Scheduled(fixedDelayString = "${sync.alfresco.delay:300000}")
    public void scheduleAlfrescoSync() {
        try {
            alfrescoSyncService.syncChangesFromAlfresco();
        } catch (Throwable t) {
            System.err.println(RED + "[FATAL] Alfresco scheduler caught unexpected error: " + t.getMessage() + RESET);
        }
    }
}