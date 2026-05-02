package mrquackduck.notEnoughAuth.services.authProviders.github;

import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.helpers.UrlBuilder;

public class GitHubAuthProvider extends AuthProvider {
    public GitHubAuthProvider(Configuration config) {
        super("GitHub",
                config.getBoolean("github.enabled"),
                "/oauth/github/callback",
                GitHubCallbackHandler.class,
                config.getString("github.clientId"),
                config.getString("github.secret"),
                "/oauth/github/callback",
                new UrlBuilder(config).getRedirectBaseUrl() + "/oauth/github/callback");
    }

    @Override
    public String buildAuthLink(String state) {
        String base = "https://github.com/login/oauth/authorize";

        String query = "client_id=" + UrlBuilder.encode(this.clientId)
                + "&state=" + UrlBuilder.encode(state)
                + "&redirect_uri=" + UrlBuilder.encode(this.redirectUrl);

        return base + "?" + query;
    }
}
