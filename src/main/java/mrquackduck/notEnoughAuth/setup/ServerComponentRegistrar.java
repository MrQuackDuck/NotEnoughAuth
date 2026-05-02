package mrquackduck.notEnoughAuth.setup;

import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.server.commands.AuthCommand;
import mrquackduck.notEnoughAuth.server.commands.LinkCommand;
import mrquackduck.notEnoughAuth.server.commands.LogoutCommand;
import mrquackduck.notEnoughAuth.server.commands.UnlinkCommand;
import mrquackduck.notEnoughAuth.server.listeners.JoinListener;

import java.util.Objects;

public abstract class ServerComponentRegistrar {
    public static void registerAll(NotEnoughAuth plugin) {
        Objects.requireNonNull(plugin.getCommand("auth")).setExecutor(new AuthCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("link")).setExecutor(new LinkCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("unlink")).setExecutor(new UnlinkCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("logout")).setExecutor(new LogoutCommand(plugin));

        var manager = plugin.getServer().getPluginManager();
        manager.registerEvents(new JoinListener(plugin), plugin);
    }
}
