package mrquackduck.notEnoughAuth.server.commands;

import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.configuration.Permissions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class InfoCommand implements CommandExecutor {
    private final Configuration config;

    public InfoCommand(NotEnoughAuth plugin) {
        this.config = new Configuration(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        StringBuilder message = new StringBuilder(config.getPlainMessage("info-header"));

        if (sender.hasPermission(Permissions.LINK))
            message.append(config.getPlainMessage("info-link"));
        if (sender.hasPermission(Permissions.UNLINK))
            message.append(config.getPlainMessage("info-unlink"));
        if (sender.hasPermission(Permissions.DEFAULT))
            message.append(config.getPlainMessage("info-logout"));
        if (sender.hasPermission(Permissions.ADMIN))
            message.append(config.getPlainMessage("info-reload"));

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message.toString()));
        return true;
    }
}