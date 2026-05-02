package mrquackduck.notEnoughAuth;

import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.OAuthHttpServerService;
import mrquackduck.notEnoughAuth.services.OAuthSessionManager;
import mrquackduck.notEnoughAuth.services.UserRepository;
import mrquackduck.notEnoughAuth.setup.PluginInitializer;
import mrquackduck.notEnoughAuth.setup.ServerComponentRegistrar;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class NotEnoughAuth extends JavaPlugin {
    private UserRepository userRepository;
    private final OAuthSessionManager sessionManager = new OAuthSessionManager();
    private OAuthHttpServerService httpServerService;
    private Configuration configuration;
    private Logger logger;

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public OAuthSessionManager getSessionManager() {
        return sessionManager;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void onEnable() {
        logger = getLogger();
        PluginInitializer.initialize(this);

        startUp();

        try { httpServerService.start(); }
        catch (Exception e) {
            logger.severe("Failed to start OAuth HTTP server: " + e.getMessage());
        }

        ServerComponentRegistrar.registerAll(this);
    }

    private void startUp() {
        this.userRepository = new UserRepository(this);
        this.configuration = new Configuration(this);
        this.httpServerService = new OAuthHttpServerService(this, configuration, userRepository, sessionManager);

        logRedirectLinks();
    }

    private void logRedirectLinks() {
        for (AuthProvider provider : configuration.authProviders()) {
            if (provider.isEnabled) {
                logger.info(provider.name + " Redirect Link → " + provider.redirectUrl);
            }
        }
    }

    public void reload() {
        reloadConfig();
        if (httpServerService != null) {
            httpServerService.stop();
            logger.info("OAuth HTTP server restarting...");
        }

        startUp();

        PluginInitializer.initialize(this);

        try {
            httpServerService.start();
        }
        catch (Exception e) {
            logger.severe("Failed to start OAuth HTTP server: " + e.getMessage());
        }

        logger.info("Plugin restarted!");
    }

    @Override
    public void onDisable() {
        if (httpServerService != null)
            httpServerService.stop();

        userRepository.onShutdown();
    }
}