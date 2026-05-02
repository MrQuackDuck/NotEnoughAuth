package mrquackduck.notEnoughAuth.setup;

import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.setup.configUpdater.ConfigUpdater;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public abstract class PluginInitializer {
    public static void initialize(NotEnoughAuth plugin) {
        plugin.saveDefaultConfig();

        // Update the config with missing key-pairs (and remove redundant ones if present)
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try { ConfigUpdater.update(plugin, "config.yml", configFile, new ArrayList<>()); }
        catch (IOException e) { plugin.getLogger().severe(e.getMessage()); }

        saveFolderIfNotExists(plugin, "webPages");
        saveDefaultResourceIfNotExists(plugin, "webPages/error.html");
        saveDefaultResourceIfNotExists(plugin, "webPages/success.html");
    }

    private static void saveFolderIfNotExists(JavaPlugin plugin, String folderPath) {
        File folder = new File(plugin.getDataFolder(), folderPath);
        if (!folder.exists()) {
            if (folder.mkdirs()) plugin.getLogger().info("Created the '" + folderPath  + "' folder.");
            else plugin.getLogger().warning("Failed to create the '" + folderPath  + "' folder.");
        }
    }

    private static void saveDefaultResourceIfNotExists(JavaPlugin plugin, String resourcePath) {
        File resource = new File(plugin.getDataFolder(), resourcePath);
        if (!resource.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }
}
