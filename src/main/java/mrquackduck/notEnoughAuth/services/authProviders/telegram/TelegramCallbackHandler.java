package mrquackduck.notEnoughAuth.services.authProviders.telegram;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.services.OAuthHttpServerService;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.OAuthCallbackHandler;
import mrquackduck.notEnoughAuth.services.helpers.HttpResponseHelper;
import mrquackduck.notEnoughAuth.utils.ParsingUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static mrquackduck.notEnoughAuth.utils.HttpClientUtil.httpPostFormWithBasicAuth;
import static mrquackduck.notEnoughAuth.utils.ParsingUtil.extractJsonPrimitiveField;
import static mrquackduck.notEnoughAuth.utils.ParsingUtil.urlEncode;

public class TelegramCallbackHandler extends OAuthCallbackHandler implements HttpHandler {
    private final OAuthHttpServerService oAuthHttpServerService;
    private final Configuration config;
    private final HttpResponseHelper httpResponseHelper;

    public TelegramCallbackHandler(OAuthHttpServerService oAuthHttpServerService, Configuration config, HttpResponseHelper httpResponseHelper) {
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

        oAuthHttpServerService.handleOAuthCallback(exchange, state, code, "telegram");
    }

    public AuthDataEntry fetchAuthData(String code, String state) throws Exception {
        String tokenEndpoint = "https://oauth.telegram.org/token";
        String redirectUri = config.telegramProvider().redirectUrl;

        String verifier = TelegramAuthProvider.consumeCodeVerifier(state);
        if (verifier == null || verifier.isEmpty()) {
            throw new IOException("Missing PKCE code_verifier for Telegram state");
        }

        String basic = Base64.getEncoder().encodeToString(
                (config.telegramProvider().clientId + ":" + config.telegramProvider().secret)
                        .getBytes(StandardCharsets.UTF_8)
        );

        String body = "grant_type=authorization_code" +
                "&code=" + urlEncode(code) +
                "&redirect_uri=" + urlEncode(redirectUri) +
                "&client_id=" + urlEncode(config.telegramProvider().clientId) +
                "&code_verifier=" + urlEncode(verifier);

        String tokenResponse = httpPostFormWithBasicAuth(
                tokenEndpoint,
                body,
                basic
        );

        String idToken = extractJsonPrimitiveField(tokenResponse, "id_token");
        if (idToken == null || idToken.isEmpty()) {
            throw new IOException("No id_token in Telegram token response");
        }

        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new IOException("Invalid Telegram id_token format");
        }

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        String sub = extractJsonPrimitiveField(payloadJson, "sub");
        if (sub == null || sub.isEmpty()) {
            sub = ParsingUtil.extractJsonPrimitiveField(payloadJson, "sub");
        }
        if (sub == null || sub.isEmpty()) {
            throw new IOException("No sub in Telegram id_token");
        }

        AuthDataEntry authDataEntry = new AuthDataEntry();
        authDataEntry.providerName = "telegram";
        authDataEntry.id = sub;

        return authDataEntry;
    }
}