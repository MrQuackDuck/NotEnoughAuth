package mrquackduck.notEnoughAuth.configuration;

import mrquackduck.notEnoughAuth.services.authProviders.discord.DiscordAuthProvider;
import mrquackduck.notEnoughAuth.services.authProviders.github.GitHubAuthProvider;
import mrquackduck.notEnoughAuth.services.authProviders.gitlab.GitLabAuthProvider;
import mrquackduck.notEnoughAuth.services.authProviders.google.GoogleAuthProvider;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.authProviders.telegram.TelegramAuthProvider;
import mrquackduck.notEnoughAuth.services.authProviders.twitch.TwitchAuthProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Configuration extends MessageConfigurationBase {
    public Configuration(JavaPlugin plugin) {
        super(plugin, "messages");
    }

    public String webIp() {
        return getString("webIp");
    }

    public int webPort() {
        return getInt("webPort");
    }

    public boolean useHttps() {
        return getBoolean("useHttps");
    }

    public String overriddenRedirectUrlBase() {
        return getString("overriddenRedirectUrlBase");
    }

    public int timeout() {
        return getInt("timeout");
    }

    public GoogleAuthProvider googleProvider() {
        return new GoogleAuthProvider(this);
    }

    public AuthProvider gitHubProvider() {
        return new GitHubAuthProvider(this);
    }

    public GitLabAuthProvider gitLabProvider() {
        return new GitLabAuthProvider(this);
    }

    public DiscordAuthProvider discordProvider() {
        return new DiscordAuthProvider(this);
    }

    public TwitchAuthProvider twitchProvider() {
        return new TwitchAuthProvider(this);
    }

    public TelegramAuthProvider telegramProvider() {
        return new TelegramAuthProvider(this);
    }

    public List<AuthProvider> authProviders() {
        return List.of(
                googleProvider(),
                gitHubProvider(),
                gitLabProvider(),
                discordProvider(),
                twitchProvider(),
                telegramProvider()
        );
    }

    public String successWebPagePath() {
        return "webPages/success.html";
    }

    public String errorWebPagePath() {
        return "webPages/error.html";
    }
}