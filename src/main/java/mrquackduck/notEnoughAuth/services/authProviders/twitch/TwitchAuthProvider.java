package mrquackduck.notEnoughAuth.services.authProviders.twitch;

import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.helpers.UrlBuilder;

public class TwitchAuthProvider extends AuthProvider {
    public TwitchAuthProvider(Configuration config) {
        super(
                "Twitch",
                config.getBoolean("twitch.enabled"),
                "/oauth/twitch/callback",
                TwitchCallbackHandler.class,
                config.getString("twitch.clientId"),
                config.getString("twitch.secret"),
                "/oauth/twitch/callback",
                new UrlBuilder(config).getRedirectBaseUrl() + "/oauth/twitch/callback"
        );
    }

    @Override
    public String buildAuthLink(String state) {
        String base = "https://id.twitch.tv/oauth2/authorize";

        String query =
                "client_id=" + UrlBuilder.encode(this.clientId)
                        + "&redirect_uri=" + UrlBuilder.encode(this.redirectUrl)
                        + "&response_type=code"
                        + "&state=" + UrlBuilder.encode(state);

        return base + "?" + query;
    }
}