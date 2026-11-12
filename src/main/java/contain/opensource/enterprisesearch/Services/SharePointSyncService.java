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

    public SharePointSyncService(
            IndexingService indexingService,
            @Value("${sharepoint.graph.base-url:https://graph.microsoft.com/v1.0}") String graphBaseUrl,
            @Value("${sharepoint.client.id}") String clientId,
            @Value("${sharepoint.client.secret}") String clientSecret,
            @Value("${sharepoint.tenant.id}") String tenantId,
            @Value("${sharepoint.graph.base-url}") String baseurl) {

        this.indexingService = indexingService;
        this.graphBaseUrl = graphBaseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
        this.GraphBaseURL = baseurl;
    }

public void syncChangesFromSharePoint() {
    System.out.println(
            GREEN + Instant.now() + " [INFO] Starting scheduled SHAREPOINT to Elasticsearch sync" + RESET);

    try {
        // Vraag token op via de instance variabelen
        String accessToken = getGraphToken();
        if (accessToken == null) {
            System.err.println(RED + "[ERROR] Failed to obtain Microsoft Graph access token. Skipping SharePoint sync." + RESET);
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

        System.out.println(GREEN + "[INFO] SharePoint sync completed successfully. Total processed: " + activeDocs.size() + RESET);

    } catch (Exception e) {
        // Vangt élke onverwachte fout af (netwerk, MSAL, JSON, etc.) 
        // Zorgt ervoor dat de methode netjes afsluit en de scheduler blijft leven!
        System.err.println(RED + "[ERROR] Error during SharePoint synchronization: " + e.getMessage() + RESET);
        
        // Optioneel tijdens debuggen om de volledige root cause (zoals de HTTP 400 HTML fout) te zien:
        // e.printStackTrace();
    }
}
    // Niet langer static, zodat hij gebruik kan maken van de ingelezen properties
    public String getGraphToken() throws Exception {
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                clientId,
                ClientCredentialFactory.createFromSecret(clientSecret))
                .authority("https://login.microsoftonline.com/" + tenantId)
                .build();

        Set<String> scopes = Collections.singleton(graphBaseUrl + "/.default");
        SilentParameters silentParameters = SilentParameters.builder(scopes).build();

        try {
            // Probeer stil en via cache op te halen (faalt logischerwijs de 1e keer)
            IAuthenticationResult result = app.acquireTokenSilently(silentParameters).get();
            System.out.println("Token retrieved from cache.");
            return result.accessToken();
        } catch (Exception e) {
            System.out.println("Cache empty or expired. Acquiring fresh token from Azure AD...");
            try {
                ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(scopes)
                        .build();
                IAuthenticationResult result = app.acquireToken(clientCredentialParameters).get();
                System.out.println("Fresh token acquired successfully!");
                return result.accessToken();
            } catch (Exception freshEx) {
                // Vang hier specifiek de fout van het ophalen van het verse token af
                Throwable cause = freshEx.getCause() != null ? freshEx.getCause() : freshEx;
                System.err.println("[ERROR] Failed to acquire fresh Azure AD token: " + cause.getMessage());
                throw new RuntimeException("Azure AD Authentication failed: " + cause.getMessage(), cause);
            }
        }
    }

    private List<EnterpriseSearchDocument> fetchDocumentsFromSharePoint(RestClient graphRestClient) {
        // (Jouw bestaande crawl logica blijft hier ongewijzigd)
        List<EnterpriseSearchDocument> allDocuments = new ArrayList<>();
        // ...
        return allDocuments;
    }
}