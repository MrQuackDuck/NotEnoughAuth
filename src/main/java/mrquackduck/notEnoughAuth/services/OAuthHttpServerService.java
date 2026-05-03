package mrquackduck.notEnoughAuth.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.models.AuthResult;
import mrquackduck.notEnoughAuth.models.OAuthSession;
import mrquackduck.notEnoughAuth.models.User;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.helpers.HttpResponseHelper;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.OAuthCallbackHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * API for handling incoming oAuth2 requests.
 */
public class OAuthHttpServerService {
    private final NotEnoughAuth plugin;
    private final Configuration config;
    private final UserRepository userRepository;
    private final OAuthSessionManager sessionManager;
    private final HttpResponseHelper httpResponseHelper;
    private final Dictionary<String, OAuthCallbackHandler> oAuthCallbackHandlers = new Hashtable<>();

    private HttpServer httpServer;

    public OAuthHttpServerService(
            NotEnoughAuth plugin,
            Configuration config,
            UserRepository userRepository,
            OAuthSessionManager sessionManager
    ) {
        this.plugin = plugin;
        this.config = config;
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
        this.httpResponseHelper = new HttpResponseHelper(plugin);
    }

    public void start() throws IOException {
        InetSocketAddress address = new InetSocketAddress(config.webPort());
        httpServer = HttpServer.create(address, 0);

        try {
            for (AuthProvider provider : config.authProviders()) {
                if (provider.isEnabled) {
                    var callbackHandler = provider.callbackHandlerType.getDeclaredConstructor(
                            OAuthHttpServerService.class,
                            Configuration.class,
                            HttpResponseHelper.class
                    ).newInstance(this, config, httpResponseHelper);

                    oAuthCallbackHandlers.put(provider.name, callbackHandler);
                    httpServer.createContext(provider.redirectPath, callbackHandler);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        httpServer.setExecutor(null);
        httpServer.start();
        plugin.getLogger().info("OAuth HTTP server started!");
    }

    public void stop() {
        if (httpServer != null) httpServer.stop(0);
    }

    public void handleOAuthCallback(HttpExchange exchange, String state, String code, String provider) throws IOException {
        OAuthSession session = requireSession(exchange, state);
        if (session == null) return;

        if (session.sessionType == OAuthSession.Type.LINK_NEW_ACCOUNT) {
            handleLinkAccount(exchange, state, code, provider);
            return;
        }

        AuthDataEntry authDataEntry = requireAuthData(exchange, state, code, provider);
        if (authDataEntry == null) return;

        String playerName = session.playerName;

        synchronized (userRepository) {
            User user = userRepository.findByPlayerName(playerName);
            boolean needToRegister = (user == null);

            if (needToRegister) {
                user = new User();
                user.playerName = playerName;
                user.activeSessionHash = session.sessionHash;
                user.activeProvider = provider;

                userRepository.upsertAuthData(user, authDataEntry);
                userRepository.save(user);

                plugin.getLogger().info("Registered new user " + playerName + " with provider " + provider + " id " + authDataEntry.id);
            }
            else {
                boolean providerIsLinked = userRepository.hasProvider(user, provider);
                if (!providerIsLinked) {
                    fail(exchange, state, 403, "auth-failed", "provider-mismatch");
                    return;
                }
                else {
                    // Validate: the account id should match the one in the database
                    if (!userRepository.matchesAccountId(user, provider, authDataEntry.id)) {
                        fail(exchange, state, 403,  "auth-failed","account-mismatch");
                        return;
                    }
                }

                userRepository.upsertAuthData(user, authDataEntry);

                user.activeSessionHash = session.sessionHash;
                user.activeProvider = provider;

                userRepository.save(user);
            }
        }

        sessionManager.completeSession(state, AuthResult.success());
        httpResponseHelper.sendSuccess(exchange, 200,
                config.getPlainMessage("auth-success"),
                config.getPlainMessage("auth-success-description"));
    }

    private void handleLinkAccount(HttpExchange exchange, String state, String code, String provider) throws IOException {
        OAuthSession session = requireSession(exchange, state);
        if (session == null) return;

        AuthDataEntry authDataEntry = requireAuthData(exchange, state, code, provider);
        if (authDataEntry == null) return;

        String playerName = session.playerName;

        synchronized (userRepository) {
            User user = userRepository.findByPlayerName(playerName);

            if (user == null) {
                fail(exchange, state, 403, "auth-failed", "not-registered");
                return;
            }

            boolean alreadyLinked = userRepository.hasProvider(user, provider);
            if (alreadyLinked) {
                fail(exchange, state, 409,  "auth-failed","provider-already-linked");
                return;
            }

            // Attach new provider
            userRepository.upsertAuthData(user, authDataEntry);
            userRepository.save(user);

            plugin.getLogger().info("Linked provider " + provider + " to user " + playerName + " with id " + authDataEntry.id);
        }

        sessionManager.completeSession(state, AuthResult.success());
        httpResponseHelper.sendSuccess(exchange, 200,
                config.getPlainMessage("auth-success"),
                config.getPlainMessage("provider-linked"));
    }

    private OAuthSession requireSession(HttpExchange exchange, String state) throws IOException {
        OAuthSession session = sessionManager.getByState(state);
        if (session == null) {
            fail(exchange, state, 400,  "auth-failed","invalid-state");
            return null;
        }
        return session;
    }

    private AuthDataEntry requireAuthData(HttpExchange exchange, String state, String code, String provider) throws IOException {
        AuthDataEntry authDataEntry;
        try {
            authDataEntry = fetchAuthDataFromProvider(code, state, provider);
        }
        catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch auth data from " + provider + ":" + e.getMessage());
            fail(exchange, state, 500, "auth-failed", "Failed to complete OAuth with " + provider + ".");
            return null;
        }

        if (authDataEntry == null || authDataEntry.id == null || authDataEntry.id.isEmpty()) {
            fail(exchange, state, 500, "auth-failed", "Could not determine user id from " + provider + ".");
            return null;
        }

        authDataEntry.providerName = provider;
        return authDataEntry;
    }

    private void fail(HttpExchange exchange, String state, int status, String titleKey, String descriptionKey) throws IOException {
        httpResponseHelper.sendError(exchange, status, config.getPlainMessage(titleKey), config.getPlainMessage(descriptionKey));
        sessionManager.completeSession(state, AuthResult.fail(config.getPlainMessage(descriptionKey)));
    }

    private AuthDataEntry fetchAuthDataFromProvider(String code, String state, String provider) throws Exception {
        for (AuthProvider authProvider : config.authProviders()) {
            if (authProvider.name.equalsIgnoreCase(provider)) {
                OAuthCallbackHandler handler = oAuthCallbackHandlers.get(authProvider.name);
                if (handler == null) throw new IllegalArgumentException("No callback handler registered for provider: " + provider);
                return handler.fetchAuthData(code, state);
            }
        }

        throw new IllegalArgumentException("Unknown provider: " + provider);
    }
}