package mrquackduck.notEnoughAuth.services.authProviders.google;

import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.helpers.UrlBuilder;

public class GoogleAuthProvider  extends AuthProvider {
    public GoogleAuthProvider(Configuration config) {
        super("Google",
                config.getBoolean("google.enabled"),
                "/oauth/google/callback",
                GoogleCallbackHandler.class,
                config.getString("google.clientId"),
                config.getString("google.secret"),
                "/oauth/google/callback",
                new UrlBuilder(config).getRedirectBaseUrl() + "/oauth/google/callback");
    }

    @Override
    public String buildAuthLink(String state) {
        String base = "https://accounts.google.com/o/oauth2/v2/auth";

        String query = "scope=" + UrlBuilder.encode("openid")
                + "&response_type=code"
                + "&redirect_uri=" + UrlBuilder.encode(redirectUrl)
                + "&client_id=" + UrlBuilder.encode(this.clientId)
                + "&state=" + UrlBuilder.encode(state);

        return base + "?" + query;
    }
}
