package mrquackduck.notEnoughAuth.services.authProviders.gitlab;

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

public class GitLabCallbackHandler extends OAuthCallbackHandler {
    private final OAuthHttpServerService oAuthHttpServerService;
    private final Configuration config;
    private final HttpResponseHelper httpResponseHelper;

    public GitLabCallbackHandler(
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

        oAuthHttpServerService.handleOAuthCallback(exchange, state, code, "gitlab");
    }

    @Override
    public AuthDataEntry fetchAuthData(String code, String state) throws Exception {
        // Exchange code for access token
        String tokenEndpoint = "https://gitlab.com/oauth/token";
        String redirectUri = config.gitLabProvider().redirectUrl;

        String body =
                "client_id=" + urlEncode(config.gitLabProvider().clientId)
                        + "&client_secret=" + urlEncode(config.gitLabProvider().secret)
                        + "&code=" + urlEncode(code)
                        + "&grant_type=authorization_code"
                        + "&redirect_uri=" + urlEncode(redirectUri);

        String tokenResponse = httpPostForm(tokenEndpoint, body, "application/x-www-form-urlencoded");

        String accessToken = ParsingUtil.extractJsonPrimitiveField(tokenResponse, "access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("No access_token in GitLab token response");
        }

        // Fetch current user
        String userEndpoint = "https://gitlab.com/api/v4/user";
        String userResponse = httpGetWithAuth(userEndpoint, accessToken, "NotEnoughAuth-GitLab");

        String id = extractJsonPrimitiveField(userResponse, "id");
        if (id == null || id.isEmpty()) {
            throw new IOException("No id in GitLab /api/v4/user response");
        }

        AuthDataEntry authDataEntry = new AuthDataEntry();
        authDataEntry.providerName = "gitlab";
        authDataEntry.id = id;

        return authDataEntry;
    }
}