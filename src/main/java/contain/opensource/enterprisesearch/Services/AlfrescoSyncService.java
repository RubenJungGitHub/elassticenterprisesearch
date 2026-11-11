package contain.opensource.enterprisesearch.services;

import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlfrescoSyncService {

    private final IndexingService indexingService;
    private final RestClient alfrescoRestClient;

    public AlfrescoSyncService(
            IndexingService indexingService,
            @Value("${alfresco.api.username:admin}") String alfrescoUser,
            @Value("${alfresco.api.password:admin}") String alfrescoPassword) {
        
        this.indexingService = indexingService;
        
        this.alfrescoRestClient = RestClient.builder()
                .baseUrl("https://alfresco-proxy-route-contain-dvl.apps.ocp0.anders-nogic.atos.net/alfresco/api/-default-/public/alfresco/versions/1")
                .defaultHeaders(headers -> headers.setBasicAuth(alfrescoUser, alfrescoPassword))
                .build();
    }

    public void syncChangesFromAlfresco() {
        System.out.println("[INFO] Starting scheduled Alfresco to Elasticsearch sync at " + Instant.now());

        try {
            List<EnterpriseSearchDocument> activeDocs = fetchRecentlyModifiedDocumentsFromAlfresco();

            if (activeDocs == null || activeDocs.isEmpty()) {
                System.out.println("[INFO] No new or modified documents found in Alfresco.");
                return;
            }

            Set<String> activeAlfrescoIds = activeDocs.stream()
                    .map(EnterpriseSearchDocument::getId)
                    .collect(Collectors.toSet());

            for (EnterpriseSearchDocument doc : activeDocs) {
                try {
                    indexingService.indexDocument(doc);
                    System.out.println("[SUCCESS] Indexed document ID: " + doc.getId() + " (" + doc.getTitle() + ")");
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to index document ID " + doc.getId() + ": " + e.getMessage());
                }
            }

            indexingService.cleanupDeletedDocumentsForSource("Alfresco", activeAlfrescoIds);

            System.out.println("[INFO] Alfresco sync completed successfully. Total processed: " + activeDocs.size());

        } catch (Exception e) {
            System.err.println("[ERROR] Error during Alfresco synchronization: " + e.getMessage());
        }
    }

    private List<EnterpriseSearchDocument> fetchRecentlyModifiedDocumentsFromAlfresco() {
        List<EnterpriseSearchDocument> allDocuments = new ArrayList<>();
        try {
            System.out.println("[INFO] Starting full Alfresco crawl across all sites and folders...");

            Map sitesResponse = alfrescoRestClient.get()
                    .uri("/sites")
                    .retrieve()
                    .body(Map.class);

            if (sitesResponse == null || !sitesResponse.containsKey("list")) {
                return Collections.emptyList();
            }

            Map sitesListMap = (Map) sitesResponse.get("list");
            List siteEntries = (List) sitesListMap.get("entries");

            if (siteEntries == null) {
                return Collections.emptyList();
            }

            for (Object rawSiteEntry : siteEntries) {
                Map siteEntry = (Map) rawSiteEntry;
                Map siteData = (Map) siteEntry.get("entry");
                String siteId = (String) siteData.get("id");
                System.out.println("[INFO] Crawling site: " + siteId);

                try {
                    Map containersResponse = alfrescoRestClient.get()
                            .uri("/sites/{siteId}/containers", siteId)
                            .retrieve()
                            .body(Map.class);

                    if (containersResponse != null && containersResponse.containsKey("list")) {
                        Map containerListMap = (Map) containersResponse.get("list");
                        List containerEntries = (List) containerListMap.get("entries");

                        if (containerEntries != null) {
                            for (Object rawContainerEntry : containerEntries) {
                                Map containerEntry = (Map) rawContainerEntry;
                                Map containerData = (Map) containerEntry.get("entry");
                                String containerId = (String) containerData.get("id");

                                if (containerId != null) {
                                    crawlNodeRecursive(containerId, allDocuments);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to fetch containers for site " + siteId + ": " + e.getMessage());
                }
            }

            System.out.println("[INFO] Full crawl completed. Total documents found: " + allDocuments.size());
            return allDocuments;

        } catch (Exception e) {
            System.err.println("[ERROR] Failed during full Alfresco crawl: " + e.getMessage());
        }

        return Collections.emptyList();
    }

    private void crawlNodeRecursive(String nodeId, List<EnterpriseSearchDocument> allDocuments) {
        try {
            Map childrenResponse = alfrescoRestClient.get()
                    .uri("/nodes/{nodeId}/children", nodeId)
                    .retrieve()
                    .body(Map.class);

            if (childrenResponse == null || !childrenResponse.containsKey("list")) {
                return;
            }

            Map listMap = (Map) childrenResponse.get("list");
            List entries = (List) listMap.get("entries");

            if (entries == null) {
                return;
            }

            for (Object rawEntry : entries) {
                Map entry = (Map) rawEntry;
                Map entryData = (Map) entry.get("entry");
                String id = (String) entryData.get("id");
                String name = (String) entryData.get("name");
                Boolean isFolder = (Boolean) entryData.get("isFolder");
                String nodeType = (String) entryData.get("nodeType");

                if (Boolean.TRUE.equals(isFolder)) {
                    crawlNodeRecursive(id, allDocuments);
                } else {
                    // Haal de permissies op via de hulpfunctie
                    List<String> allowedAuthorities = fetchNodePermissions(id);

                    EnterpriseSearchDocument doc = new EnterpriseSearchDocument();
                    doc.setId(id);
                    doc.setTitle(name);
                    doc.setFileType(nodeType);
                    doc.setCreatedDate((String) entryData.get("createdAt"));
                    doc.setContent("Alfresco document from crawl: " + name);
                    doc.setSourceSystem("Alfresco");
                    doc.setAccessControl(allowedAuthorities); // Gekoppeld aan jouw accessControl veld!

                    allDocuments.add(doc);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to crawl node children for ID " + nodeId + ": " + e.getMessage());
        }
    }

    // Hulpfunctie om permissies op te halen uit Alfresco
    private List<String> fetchNodePermissions(String nodeId) {
        List<String> authorities = new ArrayList<>();
        try {
            Map permissionsResponse = alfrescoRestClient.get()
                    .uri("/nodes/{nodeId}/permissions", nodeId)
                    .retrieve()
                    .body(Map.class);

            if (permissionsResponse != null && permissionsResponse.containsKey("entry")) {
                Map entryMap = (Map) permissionsResponse.get("entry");
                List locallySet = (List) entryMap.get("locallySet");

                if (locallySet != null) {
                    for (Object rawPerm : locallySet) {
                        Map permMap = (Map) rawPerm;
                        String authorityId = (String) permMap.get("authorityId");
                        if (authorityId != null) {
                            authorities.add(authorityId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback als het ophalen van permissies faalt
            authorities.add("GROUP_EVERYONE");
        }
        
        if (authorities.isEmpty()) {
            authorities.add("GROUP_EVERYONE");
        }

        return authorities;
    }
}