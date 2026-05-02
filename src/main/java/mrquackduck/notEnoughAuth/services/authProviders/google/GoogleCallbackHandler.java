package mrquackduck.notEnoughAuth.services.authProviders.google;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.services.helpers.HttpResponseHelper;
import mrquackduck.notEnoughAuth.services.OAuthHttpServerService;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.OAuthCallbackHandler;
import mrquackduck.notEnoughAuth.utils.HttpClientUtil;
import mrquackduck.notEnoughAuth.utils.ParsingUtil;

import java.io.IOException;
import java.util.Map;

import static mrquackduck.notEnoughAuth.utils.ParsingUtil.extractJsonPrimitiveField;
import static mrquackduck.notEnoughAuth.utils.ParsingUtil.urlEncode;

public class GoogleCallbackHandler extends OAuthCallbackHandler implements HttpHandler {
    private final OAuthHttpServerService oAuthHttpServerService;
    private final Configuration config;
    private final HttpResponseHelper httpResponseHelper;

    public GoogleCallbackHandler(OAuthHttpServerService oAuthHttpServerService, Configuration config, HttpResponseHelper httpResponseHelper) {
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

        oAuthHttpServerService.handleOAuthCallback(exchange, state, code, "google");
    }

    @Override
    public AuthDataEntry fetchAuthData(String code, String state) throws Exception {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        String redirectUri = config.googleProvider().redirectUrl;

        String body = "client_id=" + urlEncode(config.googleProvider().clientId) +
                "&client_secret=" + urlEncode(config.googleProvider().secret) +
                "&code=" + urlEncode(code) +
                "&redirect_uri=" + urlEncode(redirectUri) +
                "&grant_type=authorization_code";

        String tokenResponse = HttpClientUtil.httpPostForm(tokenEndpoint, body, "application/x-www-form-urlencoded");

        String accessToken = extractJsonPrimitiveField(tokenResponse, "access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("No access_token in Google token response");
        }

        String userInfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + urlEncode(accessToken);
        String userInfoResponse = HttpClientUtil.httpGet(userInfoUrl, "application/json", "NotEnoughAuth-Google");

        String id = extractJsonPrimitiveField(userInfoResponse, "id");
        if (id == null || id.isEmpty()) {
            throw new IOException("No id in Google userinfo response");
        }

        AuthDataEntry authDataEntry = new AuthDataEntry();
        authDataEntry.providerName = "google";
        authDataEntry.id = id;

        return authDataEntry;
    }
}
