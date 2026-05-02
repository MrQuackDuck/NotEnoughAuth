package mrquackduck.notEnoughAuth.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public abstract class ConfigurationBase {
    private final JavaPlugin plugin;

    public ConfigurationBase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String getString(String path) {
        return getConfig().getString(path);
    }

    public int getInt(String path) {
        return getConfig().getInt(path);
    }

    public boolean getBoolean(String path) {
        return getConfig().getBoolean(path);
    }

    public double getDouble(String path) {
        return getConfig().getDouble(path);
    }

    public long getLong(String path) {
        return getConfig().getLong(path);
    }

    public List<?> getList(String path) {
        return getConfig().getList(path);
    }

    public <T extends Enum<T>> T getEnumValue(String path, Class<T> enumClass, T defaultValue) {
        String value = getString(path);
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Invalid enum value '" + value + "' at path: '"+ path + "'. Using default: '" + defaultValue + "'");
            return defaultValue;
        }
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}