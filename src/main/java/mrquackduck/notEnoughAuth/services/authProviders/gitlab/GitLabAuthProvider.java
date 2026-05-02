package mrquackduck.notEnoughAuth.services.authProviders.gitlab;

import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.helpers.UrlBuilder;

public class GitLabAuthProvider extends AuthProvider {
    public GitLabAuthProvider(Configuration config) {
        super(
                "GitLab",
                config.getBoolean("gitlab.enabled"),
                "/oauth/gitlab/callback",
                GitLabCallbackHandler.class,
                config.getString("gitlab.clientId"),
                config.getString("gitlab.secret"),
                "/oauth/gitlab/callback",
                new UrlBuilder(config).getRedirectBaseUrl() + "/oauth/gitlab/callback"
        );
    }

    @Override
    public String buildAuthLink(String state) {
        String base = "https://gitlab.com/oauth/authorize";

        String query =
                "client_id=" + UrlBuilder.encode(this.clientId)
                        + "&redirect_uri=" + UrlBuilder.encode(this.redirectUrl)
                        + "&response_type=code"
                        + "&scope=" + UrlBuilder.encode("read_user")
                        + "&state=" + UrlBuilder.encode(state);

        return base + "?" + query;
    }
}