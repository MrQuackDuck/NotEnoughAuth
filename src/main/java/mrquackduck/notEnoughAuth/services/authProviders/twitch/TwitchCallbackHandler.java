package mrquackduck.notEnoughAuth.services.authProviders.twitch;

import com.sun.net.httpserver.HttpExchange;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.services.OAuthHttpServerService;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.OAuthCallbackHandler;
import mrquackduck.notEnoughAuth.services.helpers.HttpResponseHelper;
import mrquackduck.notEnoughAuth.utils.HttpClientUtil;
import mrquackduck.notEnoughAuth.utils.ParsingUtil;

import java.io.IOException;
import java.util.Map;

import static mrquackduck.notEnoughAuth.utils.ParsingUtil.extractJsonPrimitiveField;
import static mrquackduck.notEnoughAuth.utils.ParsingUtil.urlEncode;

public class TwitchCallbackHandler extends OAuthCallbackHandler {
    private final OAuthHttpServerService oAuthHttpServerService;
    private final Configuration config;
    private final HttpResponseHelper httpResponseHelper;

    public TwitchCallbackHandler(
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

        oAuthHttpServerService.handleOAuthCallback(exchange, state, code, "twitch");
    }

    @Override
    public AuthDataEntry fetchAuthData(String code, String state) throws Exception {
        String tokenEndpoint = "https://id.twitch.tv/oauth2/token";
        String redirectUri = config.twitchProvider().redirectUrl;

        String body =
                "client_id=" + urlEncode(config.twitchProvider().clientId)
                        + "&client_secret=" + urlEncode(config.twitchProvider().secret)
                        + "&code=" + urlEncode(code)
                        + "&grant_type=authorization_code"
                        + "&redirect_uri=" + urlEncode(redirectUri);

        String tokenResponse = HttpClientUtil.httpPostForm(tokenEndpoint, body, "application/x-www-form-urlencoded");

        String accessToken = ParsingUtil.extractJsonPrimitiveField(tokenResponse, "access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("No access_token in Twitch token response");
        }

        String usersUrl = "https://api.twitch.tv/helix/users";

        String usersResponse = HttpClientUtil.httpGet(
                usersUrl,
                "application/json",
                "NotEnoughAuth-Twitch",
                Map.of(
                        "Authorization", "Bearer " + accessToken,
                        "Client-Id", config.twitchProvider().clientId
                )
        );

        String firstUserObj = extractFirstHelixUserObject(usersResponse);
        if (firstUserObj == null) {
            throw new IOException("No user object in Twitch /helix/users response");
        }

        String id = extractJsonPrimitiveField(firstUserObj, "id");
        if (id == null || id.isEmpty()) {
            throw new IOException("No id in Twitch user object");
        }

        AuthDataEntry authDataEntry = new AuthDataEntry();
        authDataEntry.providerName = "twitch";
        authDataEntry.id = id;

        return authDataEntry;
    }

    private static String extractFirstHelixUserObject(String json) {
        if (json == null) return null;

        int dataIdx = json.indexOf("\"data\"");
        if (dataIdx == -1) return null;

        int arrStart = json.indexOf('[', dataIdx);
        if (arrStart == -1) return null;

        int objStart = json.indexOf('{', arrStart);
        if (objStart == -1) return null;

        // naive object end: first '}' after objStart (works for Helix user object because it's flat)
        int objEnd = json.indexOf('}', objStart);
        if (objEnd == -1) return null;

        return json.substring(objStart, objEnd + 1);
    }
}