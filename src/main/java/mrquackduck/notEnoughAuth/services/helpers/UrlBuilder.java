package mrquackduck.notEnoughAuth.services.helpers;

import mrquackduck.notEnoughAuth.configuration.Configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlBuilder {
    private final Configuration config;

    public UrlBuilder(Configuration config) {
        this.config = config;
    }

    public String getRedirectBaseUrl() {
        // Return overridden URL if enabled
        var overriddenBaseUrl = config.overriddenRedirectUrlBase();
        if (overriddenBaseUrl != null && !overriddenBaseUrl.isEmpty())
            return overriddenBaseUrl;

        StringBuilder url = new StringBuilder();

        url.append(config.useHttps() ? "https://" : "http://");
        url.append(config.webIp());

        if (config.webPort() != 80 && config.webPort() != 443) {
            url.append(":").append(config.webPort());
        }

        return url.toString();
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}