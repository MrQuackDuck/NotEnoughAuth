package mrquackduck.notEnoughAuth.services.authProviders.github;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.services.helpers.HttpResponseHelper;
import mrquackduck.notEnoughAuth.services.OAuthHttpServerService;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.OAuthCallbackHandler;
import mrquackduck.notEnoughAuth.utils.ParsingUtil;

import java.io.IOException;
import java.util.Map;

import static mrquackduck.notEnoughAuth.utils.HttpClientUtil.httpGetWithAuth;
import static mrquackduck.notEnoughAuth.utils.HttpClientUtil.httpPostForm;
import static mrquackduck.notEnoughAuth.utils.ParsingUtil.*;

public class GitHubCallbackHandler extends OAuthCallbackHandler implements HttpHandler {
    private final OAuthHttpServerService oAuthHttpServerService;
    private final Configuration config;
    private final HttpResponseHelper httpResponseHelper;

    public GitHubCallbackHandler(OAuthHttpServerService oAuthHttpServerService, Configuration config, HttpResponseHelper httpResponseHelper) {
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

        oAuthHttpServerService.handleOAuthCallback(exchange, state, code, "github");
    }

    @Override
    public AuthDataEntry fetchAuthData(String code, String state) throws Exception {
        String tokenEndpoint = "https://github.com/login/oauth/access_token";
        String redirectUri = config.gitHubProvider().redirectUrl;

        String body = "client_id=" + urlEncode(config.gitHubProvider().clientId) +
                "&client_secret=" + urlEncode(config.gitHubProvider().secret) +
                "&code=" + urlEncode(code) +
                "&redirect_uri=" + urlEncode(redirectUri);

        String tokenResponse = httpPostForm(tokenEndpoint, body, "application/x-www-form-urlencoded");

        String accessToken = extractUrlEncodedField(tokenResponse, "access_token");
        if (accessToken == null || accessToken.isEmpty())
            accessToken = ParsingUtil.extractJsonPrimitiveField(tokenResponse, "access_token");

        if (accessToken == null || accessToken.isEmpty())
            throw new IOException("No access_token in GitHub token response");

        String userUrl = "https://api.github.com/user";
        String userResponse = httpGetWithAuth(userUrl, accessToken, "NotEnoughAuth-GitHub");

        String id = ParsingUtil.extractJsonPrimitiveField(userResponse, "id");
        if (id == null || id.isEmpty()) {
            throw new IOException("No id in GitHub '/user' response");
        }

        AuthDataEntry authDataEntry = new AuthDataEntry();
        authDataEntry.providerName = "github";
        authDataEntry.id = id;

        return authDataEntry;
    }
}
