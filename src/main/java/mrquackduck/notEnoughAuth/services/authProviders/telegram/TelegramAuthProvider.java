package mrquackduck.notEnoughAuth.services.authProviders.telegram;

import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.helpers.UrlBuilder;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramAuthProvider extends AuthProvider {
    private static final Map<String, String> PKCE_VERIFIERS = new ConcurrentHashMap<>();
    private static final SecureRandom RNG = new SecureRandom();

    public TelegramAuthProvider(Configuration config) {
        super("Telegram",
                config.getBoolean("telegram.enabled"),
                "/oauth/telegram/callback",
                TelegramCallbackHandler.class,
                config.getString("telegram.clientId"),
                config.getString("telegram.secret"),
                "/oauth/telegram/callback",
                new UrlBuilder(config).getRedirectBaseUrl() + "/oauth/telegram/callback");
    }

    @Override
    public String buildAuthLink(String state) {
        String base = "https://oauth.telegram.org/auth";

        // OIDC Authorization Code + PKCE (S256)
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = sha256(codeVerifier);
        PKCE_VERIFIERS.put(state, codeVerifier);

        String scope = "openid profile";
        String query = "client_id=" + UrlBuilder.encode(this.clientId)
                + "&redirect_uri=" + UrlBuilder.encode(this.redirectUrl)
                + "&response_type=code"
                + "&scope=" + UrlBuilder.encode(scope)
                + "&state=" + UrlBuilder.encode(state)
                + "&code_challenge=" + UrlBuilder.encode(codeChallenge)
                + "&code_challenge_method=S256";

        return base + "?" + query;
    }

    public static String consumeCodeVerifier(String state) {
        if (state == null) return null;
        return PKCE_VERIFIERS.remove(state);
    }

    private static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // RFC 7636 recommends 43..128 chars, unreserved charset.
        if (verifier.length() < 43) {
            StringBuilder sb = new StringBuilder(verifier);
            while (sb.length() < 43) sb.append('A');
            verifier = sb.toString();
        }
        return verifier;
    }

    private static String sha256(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to compute PKCE challenge", e);
        }
    }
}