package mrquackduck.notEnoughAuth.services.authProviders.discord;

import com.sun.net.httpserver.HttpExchange;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.services.OAuthHttpServerService;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.OAuthCallbackHandler;
import mrquackduck.notEnoughAuth.services.helpers.HttpResponseHelper;
import mrquackduck.notEnoughAuth.utils.ParsingUtil;

import java.io.IOException;
import java.util.Map;

import static mrquackduck.notEnoughAuth.utils.HttpClientUtil.httpGetWithAuth;
import static mrquackduck.notEnoughAuth.utils.HttpClientUtil.httpPostForm;
import static mrquackduck.notEnoughAuth.utils.ParsingUtil.extractJsonPrimitiveField;
import static mrquackduck.notEnoughAuth.utils.ParsingUtil.urlEncode;

public class DiscordCallbackHandler extends OAuthCallbackHandler {
    private final OAuthHttpServerService oAuthHttpServerService;
    private final Configuration config;
    private final HttpResponseHelper httpResponseHelper;

    public DiscordCallbackHandler(
            OAuthHttpServerService oAuthHttpServerService,
            Configuration config,
            HttpResponseHelper httpResponseHelper
    ) {
        this.oAuthHttpServerService = oAuthHttpServerService;
        this.config = config;
        this.httpResponseHelper = httpResponseHelper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            httpResponseHelper.sendError(exchange, 405, config.getPlainMessage("auth-failed"), "Method Not Allowed");
            return;
        }

        Map<String, String> query = ParsingUtil.parseQuery(exchange.getRequestURI());
        String code = query.get("code");
        String state = query.get("state");

        if (code == null || state == null) {
            httpResponseHelper.sendError(exchange, 400, config.getPlainMessage("auth-failed"), "Missing 'code' or 'state'");
            return;
        }

        oAuthHttpServerService.handleOAuthCallback(exchange, state, code, "discord");
    }

    @Override
    public AuthDataEntry fetchAuthData(String code, String state) throws Exception {
        String tokenEndpoint = "https://discord.com/api/oauth2/token";
        String redirectUri = config.discordProvider().redirectUrl;

        String body =
                "client_id=" + urlEncode(config.discordProvider().clientId)
                        + "&client_secret=" + urlEncode(config.discordProvider().secret)
                        + "&grant_type=authorization_code"
                        + "&code=" + urlEncode(code)
                        + "&redirect_uri=" + urlEncode(redirectUri);

        String tokenResponse = httpPostForm(tokenEndpoint, body, "application/x-www-form-urlencoded");

        String accessToken = extractJsonPrimitiveField(tokenResponse, "access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("No access_token in Discord token response");
        }

        String meEndpoint = "https://discord.com/api/users/@me";
        String meResponse = httpGetWithAuth(meEndpoint, accessToken, "NotEnoughAuth-Discord");

        String id = extractJsonPrimitiveField(meResponse, "id");
        if (id == null || id.isEmpty()) {
            throw new IOException("No id in Discord /users/@me response");
        }

        AuthDataEntry authDataEntry = new AuthDataEntry();
        authDataEntry.providerName = "discord";
        authDataEntry.id = id;

        return authDataEntry;
    }
}