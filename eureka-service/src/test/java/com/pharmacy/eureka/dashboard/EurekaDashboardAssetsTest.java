package com.pharmacy.eureka.dashboard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "eureka.client.service-url.defaultZone=http://127.0.0.1:9/eureka/",
        "eureka.server.wait-time-in-ms-when-sync-empty=0"
})
class EurekaDashboardAssetsTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void dashboardLinksBundledStylesheet() throws Exception {
        HttpResponse<String> response = get("/");
        assertTrue(response.statusCode() == 200, "status " + response.statusCode());
        assertTrue(response.body().contains("href=\"eureka/css/wro.css?v=bootstrap-5.3.8-nav\""));
        assertTrue(response.body().contains("Home"));
        assertTrue(response.body().contains("Last 1000 since startup"));
        assertTrue(response.body().contains("href=\"") && response.body().contains("/lastn"));
    }

    @Test
    void stylesheetIsBootstrapPlusThemeWithCssContentType() throws Exception {
        HttpResponse<String> response = get("/eureka/css/wro.css");
        assertTrue(response.statusCode() == 200, "status " + response.statusCode());
        String contentType = response.headers().firstValue("content-type").orElse("");
        assertTrue(contentType.startsWith("text/css"), contentType);
        assertTrue(response.body().contains("Bootstrap  v5.3.8"), "missing Bootstrap");
        assertTrue(response.body().contains(".col-md-6"), "missing Bootstrap grid");
        assertTrue(response.body().contains("varela_roundregular"), "missing Eureka theme");
        int bootstrapAt = response.body().indexOf("Bootstrap  v5.3.8");
        int themeAt = response.body().indexOf("varela_roundregular");
        assertTrue(bootstrapAt >= 0 && bootstrapAt < themeAt, "theme must follow Bootstrap so Eureka rules win");
        int navFixAt = response.body().indexOf(".navbar.navbar-dark .navbar-nav .nav-link:visited");
        assertTrue(navFixAt > themeAt, "navbar visibility fix must follow Eureka theme");

        HttpRequest head = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + this.port + "/eureka/css/wro.css"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> headResponse = this.client.send(head, HttpResponse.BodyHandlers.discarding());
        assertTrue(headResponse.statusCode() == 200, "HEAD status " + headResponse.statusCode());
        String headType = headResponse.headers().firstValue("content-type").orElse("");
        assertTrue(headType.startsWith("text/css"), headType);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + this.port + path)).build();
        return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
