package contain.opensource.enterprisesearch.Schedulers;

import contain.opensource.enterprisesearch.services.SharePointSyncService;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import static contain.opensource.enterprisesearch.util.AnsiColor.*;

@Component
public class SharePointSyncScheduler {

    private final SharePointSyncService sharePointSyncService;

    public SharePointSyncScheduler(SharePointSyncService sharePointSyncService) {
        this.sharePointSyncService = sharePointSyncService;
    }

    @Scheduled(fixedDelayString = "${sync.sharepoint.delay:30000}")
    public void scheduleSharePointSync() {
        try {
            sharePointSyncService.syncChangesFromSharePoint();
        } catch (Throwable t) {
            // Absolute noodrem, mocht er ooit iets geks doorheen glippen
            System.err.println(RED_BG + "[FATAL] Scheduler caught unexpected error: " + t.getMessage() + RESET);
        }
    }
}