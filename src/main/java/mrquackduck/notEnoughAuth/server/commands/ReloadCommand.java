package mrquackduck.notEnoughAuth.server.commands;

import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class ReloadCommand implements CommandExecutor {
    private final NotEnoughAuth plugin;
    private final Configuration config;

    public ReloadCommand(NotEnoughAuth plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        // Signaling the plugin to reload
        try {
            plugin.reload();
        }
        catch (Exception e) {
            return handleException(commandSender, e);
        }

        commandSender.sendMessage(config.getMessage("reloaded"));

        return true;
    }

    private boolean handleException(@NotNull CommandSender commandSender, Exception e) {
        commandSender.sendMessage(config.getMessage("an-error-occurred"));
        plugin.getLogger().log(Level.SEVERE, e.getMessage());
        return true;
    }
}
