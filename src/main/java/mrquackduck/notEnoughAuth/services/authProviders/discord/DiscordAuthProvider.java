package mrquackduck.notEnoughAuth.services.authProviders.discord;

import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.helpers.UrlBuilder;

public class DiscordAuthProvider extends AuthProvider {
    public DiscordAuthProvider(Configuration config) {
        super(
                "Discord",
                config.getBoolean("discord.enabled"),
                "/oauth/discord/callback",
                DiscordCallbackHandler.class,
                config.getString("discord.clientId"),
                config.getString("discord.secret"),
                "/oauth/discord/callback",
                new UrlBuilder(config).getRedirectBaseUrl() + "/oauth/discord/callback"
        );
    }

    @Override
    public String buildAuthLink(String state) {
        String base = "https://discord.com/api/oauth2/authorize";

        String query =
                "client_id=" + UrlBuilder.encode(this.clientId)
                        + "&redirect_uri=" + UrlBuilder.encode(this.redirectUrl)
                        + "&response_type=code"
                        + "&scope=" + UrlBuilder.encode("identify")
                        + "&state=" + UrlBuilder.encode(state);

        return base + "?" + query;
    }
}