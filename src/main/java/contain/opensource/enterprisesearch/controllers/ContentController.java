package contain.opensource.enterprisesearch.controllers;

import java.io.OutputStream;
import java.time.Instant;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@RestController
public class ContentController {

    private final RestClient alfrescoRestClient;

    final String ANSI_GREEN_BG  = "\u001B[42m";
    final String ANSI_RED_BG    = "\u001B[41m";
    final String ANSI_YELLOW_BG = "\u001B[43m";
    final String ANSI_BLACK     = "\u001B[30m";
    final String ANSI_RESET     = "\u001B[0m";

    public ContentController(
            @Value("${alfresco.api.url}") String alfrescoUrl,
            @Value("${alfresco.api.username:admin}") String alfrescoUser,
            @Value("${alfresco.api.password:admin}") String alfrescoPassword) {

        String baseUrl = alfrescoUrl.endsWith("/") ? alfrescoUrl.substring(0, alfrescoUrl.length() - 1) : alfrescoUrl;

        this.alfrescoRestClient = RestClient.builder()
                .baseUrl(baseUrl + "/alfresco/api/-default-/public/alfresco/versions/1")
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(alfrescoUser, alfrescoPassword);
                    headers.set(HttpHeaders.CONNECTION, "close");
                })
                .build();
    }

    @GetMapping("/api/files/{id}/content")
    public void proxyAlfrescoContent(@PathVariable String id, HttpServletResponse response) {
        try {
            System.out.println(ANSI_YELLOW_BG + ANSI_BLACK + " " + Instant.now() + " [PROXY] Fetching content for node ID: " + id + ANSI_RESET);
            
            org.springframework.http.ResponseEntity<byte[]> alfrescoResponse = alfrescoRestClient.get()
                    .uri("/nodes/{id}/content", id)
                    .headers(headers -> headers.remove(HttpHeaders.RANGE))
                    .retrieve()
                    .toEntity(byte[].class);

            byte[] body = alfrescoResponse.getBody();
            if (body == null) {
                body = new byte[0];
            }

            // Kopieer headers uit Alfresco (behalve problematische lengtes en ranges)
            if (alfrescoResponse.getHeaders() != null) {
                alfrescoResponse.getHeaders().forEach((key, values) -> {
                    if (!key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH) &&
                        !key.equalsIgnoreCase(HttpHeaders.CONTENT_RANGE) &&
                        !key.equalsIgnoreCase(HttpHeaders.ACCEPT_RANGES)) {
                        for (String value : values) {
                            response.addHeader(key, value);
                        }
                    }
                });
            }

            // Forceer inline weergave i.p.v. download
            String contentDisp = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
            if (contentDisp != null) {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisp.replace("attachment", "inline"));
            } else {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
            }

            // Stel status en exacte lengte in
            response.setStatus(alfrescoResponse.getStatusCode().value());
            response.setContentLength(body.length);

            // Schrijf direct naar de outputstream van de servlet (omzeilt Jetty's ResponseEntity validatie)
            try (OutputStream out = response.getOutputStream()) {
                out.write(body);
                out.flush();
            }

            System.out.println(ANSI_GREEN_BG + ANSI_BLACK + " [SUCCESS] Directly streamed content for node ID: " + id + " (" + body.length + " bytes) " + ANSI_RESET);

        } catch (HttpClientErrorException e) {
            System.err.println(ANSI_RED_BG + ANSI_BLACK + " [PROXY ERROR] Alfresco responded with status " + e.getStatusCode() + " " + ANSI_RESET);
            try {
                response.sendError(e.getStatusCode().value(), e.getStatusText());
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.err.println(ANSI_RED_BG + ANSI_BLACK + " [PROXY ERROR] Unexpected error: " + e.getMessage() + " " + ANSI_RESET);
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (Exception ignored) {}
        }
    }
}