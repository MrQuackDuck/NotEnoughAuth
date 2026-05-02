package mrquackduck.notEnoughAuth.server.commands;

import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.configuration.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AuthCommand implements CommandExecutor, TabCompleter {
    private final NotEnoughAuth plugin;
    private final Configuration config;

    public AuthCommand(NotEnoughAuth plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            // Print info if command args were not provided
            return new InfoCommand(plugin).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("link") && commandSender.hasPermission(Permissions.LINK)) {
            return new LinkCommand(plugin).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("unlink") && commandSender.hasPermission(Permissions.UNLINK)) {
            return new UnlinkCommand(plugin).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("logout") && commandSender.hasPermission(Permissions.DEFAULT)) {
            return new LogoutCommand(plugin).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("reload") && commandSender.hasPermission(Permissions.ADMIN)) {
            return new ReloadCommand(plugin).onCommand(commandSender, command, s, args);
        }
        else {
            commandSender.sendMessage(config.getMessage("command-not-found"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        List<String> options = new ArrayList<>();
        List<String> completions = new ArrayList<>();

        options.add("info");
        if (commandSender.hasPermission(Permissions.DEFAULT)) options.add("logout");
        if (commandSender.hasPermission(Permissions.LINK)) options.add("link");
        if (commandSender.hasPermission(Permissions.UNLINK)) options.add("unlink");
        if (commandSender.hasPermission(Permissions.ADMIN)) options.add("reload");

        StringUtil.copyPartialMatches(args[0], options, completions);
        return completions;
    }
}
