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
import static contain.opensource.enterprisesearch.util.AnsiColor.*;
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
                .baseUrl(
                        "https://alfresco-proxy-route-contain-dvl.apps.ocp0.anders-nogic.atos.net/alfresco/api/-default-/public/alfresco/versions/1")
                .defaultHeaders(headers -> headers.setBasicAuth(alfrescoUser, alfrescoPassword))
                .build();
    }

    public void syncChangesFromAlfresco() {
   System.out.println(
            GREEN_BG + Instant.now() + " [INFO] Starting scheduled ALFRESCO to Elasticsearch sync" + RESET);



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
                                    // Start de recursieve crawl met een leeg pad (root van de container)
                                    crawlNodeRecursive(containerId, siteId, "", allDocuments);
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

    private void crawlNodeRecursive(String nodeId, String siteId, String currentPath, List<EnterpriseSearchDocument> allDocuments) {
        try {
            Map childrenResponse = alfrescoRestClient.get()
                    .uri("/nodes/{nodeId}/children?include=properties", nodeId)
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
                    // Bouw het map-pad verder uit (bijv. /Unclassified of /MapA/MapB)
                    String subPath = currentPath.isEmpty() ? "/" + name : currentPath + "/" + name;
                    crawlNodeRecursive(id, siteId, subPath, allDocuments);
                } else {
                    List<String> allowedAuthorities = fetchNodePermissions(id);
                    String proxyUrl = "/api/files/" + id + "/content";
                    
                    // Bouw de Share URL op die eindigt in de exacte submap waar het bestand staat
                    // Hiermee opent Alfresco Share direct de juiste folder via de path filter hash
                    String folderUrl = "https://alfresco-proxy-route-contain-dvl.apps.ocp0.anders-nogic.atos.net/share/page/site/" + siteId + "/documentlibrary#filter=path|" + currentPath;

                    Map<String, Object> properties = (Map<String, Object>) entryData.get("properties");
                    String classification = null;
                    if (properties != null) {
                        classification = (String) properties.get("contain:EXTTAXCLASSIFICATIONVAL");
                        if (classification == null) {
                            classification = (String) properties.get("ils:classification");
                        }
                        if (classification == null) {
                            classification = (String) properties.get("classification");
                        }
                    }
                    if (classification == null) {
                        classification = (String) entryData.get("classification");
                    }

                    EnterpriseSearchDocument doc = new EnterpriseSearchDocument();
                    doc.setId(id);
                    doc.setTitle(name);
                    doc.setFileType(nodeType);
                    doc.setCreatedDate((String) entryData.get("createdAt"));
                    doc.setClassification(classification);
                    doc.setContent("Alfresco document from crawl: " + name);
                    doc.setSourceSystem("Alfresco");
                    doc.setUrl(proxyUrl);
                    doc.setFolderUrl(folderUrl); // Bevat nu het exacte pad (bijv. #filter=path|/Unclassified)
                    doc.setAccessControl(allowedAuthorities);

                    allDocuments.add(doc);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to crawl node children for ID " + nodeId + ": " + e.getMessage());
        }
    }

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
            authorities.add("GROUP_EVERYONE");
        }

        if (authorities.isEmpty()) {
            authorities.add("GROUP_EVERYONE");
        }

        return authorities;
    }
}