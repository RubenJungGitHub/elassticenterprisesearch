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

@Service
public class AlfrescoSyncService {

    private final IndexingService indexingService;

    private final RestClient alfrescoRestClient;

public AlfrescoSyncService(
            IndexingService indexingService,
            @Value("${alfresco.api.username:admin}") String alfrescoUser,
            @Value("${alfresco.api.password:admin}") String alfrescoPassword) {
        
        this.indexingService = indexingService;
        
        // Initialiseer de RestClient met de correct ingelezen properties
        this.alfrescoRestClient = RestClient.builder()
                .baseUrl("https://alfresco-proxy-route-contain-dvl.apps.ocp0.anders-nogic.atos.net/alfresco/api/-default-/public/alfresco/versions/1")
                .defaultHeaders(headers -> headers.setBasicAuth(alfrescoUser, alfrescoPassword))
                .build();
    }

    public void syncChangesFromAlfresco() {
        System.out.println("[INFO] Starting scheduled Alfresco to Elasticsearch sync at " + Instant.now());

        try {
            // 1. Haal gewijzigde documenten op via de volledige crawler
            List<EnterpriseSearchDocument> changedDocs = fetchRecentlyModifiedDocumentsFromAlfresco();

            if (changedDocs == null || changedDocs.isEmpty()) {
                System.out.println("[INFO] No new or modified documents found in Alfresco.");
                return;
            }

            // 2. Loop door de documenten en push ze naar Elasticsearch
            for (EnterpriseSearchDocument doc : changedDocs) {
                try {
                    indexingService.indexDocument(doc);
                    System.out.println("[SUCCESS] Indexed document ID: " + doc.getId() + " (" + doc.getTitle() + ")");
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
        List<EnterpriseSearchDocument> allDocuments = new ArrayList<>();
        try {
            System.out.println("[INFO] Starting full Alfresco crawl across all sites and folders...");

            Map<String, Object> sitesResponse = alfrescoRestClient.get()
                    .uri("/sites")
                    .retrieve()
                    .body(Map.class);

            if (sitesResponse == null || !sitesResponse.containsKey("list")) {
                return Collections.emptyList();
            }

            Map<String, Object> sitesListMap = (Map<String, Object>) sitesResponse.get("list");
            List<Map<String, Object>> siteEntries = (List<Map<String, Object>>) sitesListMap.get("entries");

            if (siteEntries == null) {
                return Collections.emptyList();
            }

            for (Map<String, Object> siteEntry : siteEntries) {
                Map<String, Object> siteData = (Map<String, Object>) siteEntry.get("entry");
                String siteId = (String) siteData.get("id");
                System.out.println("[INFO] Crawling site: " + siteId);

                try {
                    Map<String, Object> containersResponse = alfrescoRestClient.get()
                            .uri("/sites/{siteId}/containers", siteId)
                            .retrieve()
                            .body(Map.class);

                    if (containersResponse != null && containersResponse.containsKey("list")) {
                        Map<String, Object> containerListMap = (Map<String, Object>) containersResponse.get("list");
                        List<Map<String, Object>> containerEntries = (List<Map<String, Object>>) containerListMap
                                .get("entries");
                        // List<Map<String, Object>> containerEntries = (List<Map<String, Object>>)
                        // sitesListMap.get("entries");

                        if (containerEntries != null) {
                            for (Map<String, Object> containerEntry : containerEntries) {
                                Map<String, Object> containerData = (Map<String, Object>) containerEntry.get("entry");
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

    // Hulpfunctie om recursief door mappen en bestanden te lopen
    private void crawlNodeRecursive(String nodeId, List<EnterpriseSearchDocument> allDocuments) {
        try {
            Map<String, Object> childrenResponse = alfrescoRestClient.get()
                    .uri("/nodes/{nodeId}/children", nodeId)
                    .retrieve()
                    .body(Map.class);

            if (childrenResponse == null || !childrenResponse.containsKey("list")) {
                return;
            }

            Map<String, Object> listMap = (Map<String, Object>) childrenResponse.get("list");
            List<Map<String, Object>> entries = (List<Map<String, Object>>) listMap.get("entries");

            if (entries == null) {
                return;
            }

            for (Map<String, Object> entry : entries) {
                Map<String, Object> entryData = (Map<String, Object>) entry.get("entry");
                String id = (String) entryData.get("id");
                String name = (String) entryData.get("name");
                Boolean isFolder = (Boolean) entryData.get("isFolder");
                String nodeType = (String) entryData.get("nodeType");

                if (Boolean.TRUE.equals(isFolder)) {
                    // Het is een map: roep deze methode zichzelf opnieuw aan voor de submap (recursie)
                    crawlNodeRecursive(id, allDocuments);
                } else {
                    // Het is een bestand: voeg toe aan de lijst voor Elasticsearch
                    EnterpriseSearchDocument doc = new EnterpriseSearchDocument();
                    doc.setId(id);
                    doc.setTitle(name);
                    doc.setFileType(nodeType);
                    doc.setCreatedDate((String) entryData.get("createdAt"));
                    doc.setContent("Alfresco document from crawl: " + name);

                    allDocuments.add(doc);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to crawl node children for ID " + nodeId + ": " + e.getMessage());
        }
    }
}
