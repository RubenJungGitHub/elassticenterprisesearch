package contain.opensource.enterprisesearch.config;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrosoftGraphConfig {

    @Bean
    public ConfidentialClientApplication msalClient(
            @Value("${sharepoint.client.id}") String clientId,
            @Value("${sharepoint.client.secret}") String clientSecret,
            @Value("${sharepoint.tenant.id}") String tenantId) {
        try {
            return ConfidentialClientApplication.builder(
                    clientId,
                    ClientCredentialFactory.createFromSecret(clientSecret))
                    .authority("https://login.microsoftonline.com/" + tenantId)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MSAL ConfidentialClientApplication", e);
        }
    }
}