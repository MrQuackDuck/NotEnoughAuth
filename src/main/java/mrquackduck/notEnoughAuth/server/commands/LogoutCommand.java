package mrquackduck.notEnoughAuth.server.commands;

import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LogoutCommand implements CommandExecutor {
    private final NotEnoughAuth plugin;
    private final Configuration config;

    public LogoutCommand(NotEnoughAuth plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(config.getMessage("only-players"));
            return true;
        }

        plugin.getUserRepository().logOut(commandSender.getName());
        player.kick(config.getMessage("logged-out"));

        return true;
    }
}
