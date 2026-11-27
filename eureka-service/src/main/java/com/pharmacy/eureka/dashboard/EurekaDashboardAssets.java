package com.pharmacy.eureka.dashboard;



import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Restores the Eureka dashboard layout after the Spring Cloud Netflix 5.1 upgrade.
 * <p>
 * {@code spring-cloud-netflix-eureka-server} 5.1.0 still builds {@code wro.css} from
 * {@code webjar:bootstrap/5.1.3}, but the module depends on Bootstrap 5.3.8. The published
 * stylesheet is only the custom theme (about 5 KB) and {@code wro.js} is empty, so the
 * Bootstrap grid, tables, and navbar never reach the browser. The dashboard HTML already
 * links these URLs; this controller serves Bootstrap ahead of the theme and sets an explicit
 * stylesheet content type.
 */
@Controller
public class EurekaDashboardAssets {

    private static final byte[] BOOTSTRAP_MARKER = "bootstrap".getBytes(StandardCharsets.US_ASCII);

    /**
     * Eureka's theme sets {@code .nav-link { color: #34302d }} — the same color as the
     * header. That rule has the same specificity as Bootstrap 5.3 {@code .nav-link} and
     * wins because it is concatenated last, so Home and Last 1000 disappear on the dark bar.
     * {@code .navbar .nav-link:link} only restyles unvisited links.
     */
    private static final byte[] NAVBAR_VISIBILITY = """

            .navbar.navbar-dark .navbar-nav .nav-link,
            .navbar.navbar-dark .navbar-nav .nav-link:link,
            .navbar.navbar-dark .navbar-nav .nav-link:visited {
                color: #f1f1f1;
                text-transform: uppercase;
            }
            .navbar.navbar-dark .navbar-nav .nav-link:hover,
            .navbar.navbar-dark .navbar-nav .nav-link:focus {
                color: #ffffff;
                background-color: #6db33f;
            }
            @media (min-width: 768px) {
                .navbar-expand-md .navbar-collapse.collapse {
                    display: flex !important;
                    flex-basis: auto;
                }
            }
            """.getBytes(StandardCharsets.UTF_8);

    private final byte[] css;
    private final byte[] javascript;

    public EurekaDashboardAssets(ResourcePatternResolver resources) throws IOException {
        byte[] theme = read(resources.getResource("classpath:static/eureka/css/wro.css"));
        byte[] withBootstrap;
        if (contains(theme, BOOTSTRAP_MARKER)) {
            withBootstrap = theme;
        }
        else {
            byte[] bootstrapCss = read(webjar(resources, "dist/css/bootstrap.min.css"));
            withBootstrap = concat(bootstrapCss, theme);
        }
        this.css = concat(withBootstrap, NAVBAR_VISIBILITY);

        byte[] bundledJs = read(resources.getResource("classpath:static/eureka/js/wro.js"));
        if (contains(bundledJs, BOOTSTRAP_MARKER)) {
            this.javascript = bundledJs;
        }
        else {
            this.javascript = read(webjar(resources, "dist/js/bootstrap.bundle.min.js"));
        }
    }

    @RequestMapping(path = "/eureka/css/wro.css", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void css(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, "text/css", this.css);
    }

    @RequestMapping(path = "/eureka/js/wro.js", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void javascript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, "text/javascript", this.javascript);
    }

    private static void write(HttpServletRequest request, HttpServletResponse response, String contentType,
            byte[] body) throws IOException {
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache, must-revalidate");
        response.setDateHeader("Expires", 0);
        response.setContentLength(body.length);
        if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
            response.getOutputStream().write(body);
        }
    }

    private static Resource webjar(ResourcePatternResolver resources, String suffix) throws IOException {
        Resource[] found = resources.getResources("classpath*:META-INF/resources/webjars/bootstrap/*/" + suffix);
        if (found.length == 0) {
            throw new IllegalStateException("Bootstrap webjar resource not found: " + suffix);
        }
        Arrays.sort(found, Comparator.comparing(Resource::getDescription));
        return found[found.length - 1];
    }

    private static byte[] read(Resource resource) throws IOException {
        if (!resource.exists()) {
            throw new IllegalStateException("Missing dashboard resource " + resource);
        }
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] combined = new byte[first.length + 1 + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        combined[first.length] = '\n';
        System.arraycopy(second, 0, combined, first.length + 1, second.length);
        return combined;
    }

    private static boolean contains(byte[] body, byte[] needle) {
        if (needle.length == 0 || body.length < needle.length) {
            return false;
        }
        outer:
        for (int i = 0; i <= body.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                byte value = body[i + j];
                if (value >= 'A' && value <= 'Z') {
                    value += 32;
                }
                if (value != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
