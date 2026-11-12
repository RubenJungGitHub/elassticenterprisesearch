package contain.opensource.enterprisesearch.services;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.SilentParameters;
import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import contain.opensource.enterprisesearch.model.Site;
import contain.opensource.enterprisesearch.model.Drive;
import contain.opensource.enterprisesearch.model.DriveItem;
import contain.opensource.enterprisesearch.model.SitesResponse;
import contain.opensource.enterprisesearch.model.DrivesResponse;
import contain.opensource.enterprisesearch.model.DriveItemsResponse;
import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;

import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static contain.opensource.enterprisesearch.util.AnsiColor.*;

@Service
public class SharePointSyncService {

    private final IndexingService indexingService;
    private final String graphBaseUrl;

    private final String clientId;
    private final String clientSecret;
    private final String tenantId;
    private final String GraphBaseURL;
    private final ConfidentialClientApplication msalApp;

    public SharePointSyncService(
            IndexingService indexingService,
            ConfidentialClientApplication msalApp,
            @Value("${sharepoint.graph.base-url:https://graph.microsoft.com/v1.0}") String graphBaseUrl,
            @Value("${sharepoint.client.id}") String clientId,
            @Value("${sharepoint.client.secret}") String clientSecret,
            @Value("${sharepoint.tenant.id}") String tenantId,
            @Value("${sharepoint.graph.base-url}") String baseurl) {

        this.indexingService = indexingService;
        this.graphBaseUrl = graphBaseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.msalApp = msalApp;
        this.tenantId = tenantId;
        this.GraphBaseURL = baseurl;
        // Initialiseer de ConfidentialClientApplication eenmalig in de constructor
    }

    public void syncChangesFromSharePoint() {
        System.out.println(
                GREEN + Instant.now() + " [INFO] Starting scheduled SHAREPOINT to Elasticsearch sync" + RESET);

        try {
            // Vraag token op via de instance variabelen
            String accessToken = getGraphToken();
            if (accessToken == null) {
                System.err.println(RED
                        + "[ERROR] Failed to obtain Microsoft Graph access token. Skipping SharePoint sync." + RESET);
                return;
            }

            RestClient graphRestClient = RestClient.builder()
                    .baseUrl(graphBaseUrl)
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("Accept", "application/json")
                    .build();

            List<EnterpriseSearchDocument> activeDocs = fetchDocumentsFromSharePoint(graphRestClient);

            if (activeDocs == null || activeDocs.isEmpty()) {
                System.out.println("[INFO] No documents found in SharePoint.");
                return;
            }

            Set<String> activeSpIds = activeDocs.stream()
                    .map(EnterpriseSearchDocument::getId)
                    .collect(Collectors.toSet());

            for (EnterpriseSearchDocument doc : activeDocs) {
                try {
                    indexingService.indexDocument(doc);
                    System.out.println(
                            "[SUCCESS] Indexed SP document ID: " + doc.getId() + " (" + doc.getTitle() + ")");
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to index SP document ID " + doc.getId() + ": " + e.getMessage());
                }
            }

            indexingService.cleanupDeletedDocumentsForSource("SharePoint", activeSpIds);

            System.out.println(GREEN + "[INFO] SharePoint sync completed successfully. Total processed: "
                    + activeDocs.size() + RESET);

        } catch (Exception e) {
            // Vangt élke onverwachte fout af (netwerk, MSAL, JSON, etc.)
            // Zorgt ervoor dat de methode netjes afsluit en de scheduler blijft leven!
            System.err.println(RED + "[ERROR] Error during SharePoint synchronization: " + e.getMessage() + RESET);

            // Optioneel tijdens debuggen om de volledige root cause (zoals de HTTP 400 HTML
            // fout) te zien:
            // e.printStackTrace();
        }
    }

    // Niet langer static, zodat hij gebruik kan maken van de ingelezen properties
public String getGraphToken() throws Exception {
        Set<String> scopes = Collections.singleton(graphBaseUrl + "/.default");
        SilentParameters silentParameters = SilentParameters.builder(scopes).build();

        try {
            // Haalt nu het token op uit de cache van de persistent msalApp instantie
            IAuthenticationResult result = msalApp.acquireTokenSilently(silentParameters).get();
            System.out.println("Token retrieved from cache.");
            return result.accessToken();
        } catch (Exception e) {
            System.out.println("Cache empty or expired. Acquiring fresh token from Azure AD...");
            try {
                ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(scopes)
                        .build();
                IAuthenticationResult result = msalApp.acquireToken(clientCredentialParameters).get();
                System.out.println("Fresh token acquired successfully!");
                return result.accessToken();
            } catch (Exception freshEx) {
                Throwable cause = freshEx.getCause() != null ? freshEx.getCause() : freshEx;
                System.err.println("[ERROR] Failed to acquire fresh Azure AD token: " + cause.getMessage());
                throw new RuntimeException("Azure AD Authentication failed: " + cause.getMessage(), cause);
            }
        }
    }

    private List<EnterpriseSearchDocument> fetchDocumentsFromSharePoint(RestClient graphRestClient) {
        List<EnterpriseSearchDocument> allDocuments = new ArrayList<>();

        try {
            // 1. Haal alle SharePoint sites op in de tenant
            // Vereist 'Sites.Read.All' (Application permission)
            SitesResponse sitesResponse = graphRestClient.get()
                    .uri("/v1.0/sites?search=*")
                    .retrieve()
                    .body(SitesResponse.class);

            if (sitesResponse == null || sitesResponse.getValue() == null) {
                return allDocuments;
            }

            for (Site site : sitesResponse.getValue()) {
                // 2. Haal alle documentbibliotheken (drives) op per site
                DrivesResponse drivesResponse = graphRestClient.get()
                        .uri("/v1.0/sites/{siteId}/drives", site.getId())
                        .retrieve()
                        .body(DrivesResponse.class);

                if (drivesResponse == null || drivesResponse.getValue() == null) {
                    continue;
                }

                for (Drive drive : drivesResponse.getValue()) {
                    // Geef site.getWebUrl() mee als extra argument
                    crawlDriveItems(graphRestClient, drive.getId(), "/root/children", "", site.getWebUrl(),
                            allDocuments);
                }
            }

        } catch (Exception e) {
            // Log fout of handel afhankelijk van je error-handling
            System.err.println("Fout tijdens SharePoint crawling: " + e.getMessage());
        }

        return allDocuments;
    }

    // Hulpfunctie om recursief door mappen en bestanden te gaan
    private void crawlDriveItems(RestClient graphRestClient, String driveId, String pathSegment,
            String currentPath, String siteWebUrl, List<EnterpriseSearchDocument> allDocuments) {
        try {
            DriveItemsResponse itemsResponse = graphRestClient.get()
                    .uri("/v1.0/drives/{driveId}{pathSegment}", driveId, pathSegment)
                    .retrieve()
                    .body(DriveItemsResponse.class);

            if (itemsResponse == null || itemsResponse.getValue() == null) {
                return;
            }

            for (DriveItem item : itemsResponse.getValue()) {
                if (item.getFile() != null) {
                    EnterpriseSearchDocument doc = new EnterpriseSearchDocument();
                    doc.setId(item.getId());
                    doc.setTitle(item.getName());
                    doc.setFileType("file");
                    doc.setCreatedDate(item.getCreatedDateTime());
                    doc.setClassification("Public");
                    doc.setContent("SharePoint document from crawl: " + item.getName());
                    doc.setSourceSystem("SharePoint");
                    doc.setUrl(item.getWebUrl());

                    // Hier is 'siteWebUrl' nu netjes bekend!
                    String folderUrl = siteWebUrl + "/Shared%20Documents#filter=path|"
                            + (currentPath.isEmpty() ? "/" : currentPath);
                    doc.setFolderUrl(folderUrl);
                    doc.setFolderName(currentPath.isEmpty() ? "Shared Documents" : currentPath);

                    doc.setAccessControl(null);
                    allDocuments.add(doc);

                } else if (item.getFolder() != null) {
                    String subPath = "/items/" + item.getId() + "/children";
                    String nextPath = currentPath + "/" + item.getName();

                    // Ook hier doorgeven bij de recursieve aanroep:
                    crawlDriveItems(graphRestClient, driveId, subPath, nextPath, siteWebUrl, allDocuments);
                }
            }
        } catch (Exception e) {
            System.err.println("Kon drive items niet ophalen voor path " + pathSegment + ": " + e.getMessage());
        }
    }
}