package mrquackduck.notEnoughAuth.services;

import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.models.User;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository class for managing users.
 * Stores and processes data in 'YAML' format.
 */
public class UserRepository {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final List<User> users = new ArrayList<>();

    public UserRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadUsers();
    }

    public synchronized User findByPlayerName(String name) {
        for (User user : users) {
            if (user.playerName != null && user.playerName.equalsIgnoreCase(name)) return user;
        }
        return null;
    }

    public synchronized void logOut(String name) {
        User user = findByPlayerName(name);
        if (user == null) return;
        user.activeSessionHash = null;
        saveUsers();
    }

    public synchronized void save(User user) {
        if (!users.contains(user)) users.add(user);
        saveUsers();
    }

    public synchronized boolean hasProvider(User user, String providerName) {
        if (user == null || user.authenticationData == null) return false;
        for (AuthDataEntry authDataEntry : user.authenticationData) {
            if (authDataEntry != null && authDataEntry.providerName != null && authDataEntry.providerName.equalsIgnoreCase(providerName)) return true;
        }
        return false;
    }

    public synchronized boolean matchesAccountId(User user, String providerName, String providerUserId) {
        if (user == null || user.authenticationData == null) return false;
        for (AuthDataEntry authDataEntry : user.authenticationData) {
            if (authDataEntry == null) continue;
            if (authDataEntry.providerName != null && authDataEntry.providerName.equalsIgnoreCase(providerName)) {
                return authDataEntry.id != null && authDataEntry.id.equals(providerUserId);
            }
        }
        return false;
    }

    /**
     * Insert if missing, else update existing record.
     */
    public synchronized void upsertAuthData(User user, AuthDataEntry incoming) {
        if (user.authenticationData == null) user.authenticationData = new ArrayList<>();
        if (incoming == null) return;

        // Normalize provider name
        if (incoming.providerName == null) incoming.providerName = "";

        for (AuthDataEntry existing : user.authenticationData) {
            if (existing == null) continue;

            if (existing.providerName != null && existing.providerName.equalsIgnoreCase(incoming.providerName)) {
                existing.id = incoming.id;

                return;
            }
        }

        // Not found, add new
        AuthDataEntry copy = new AuthDataEntry();
        copy.providerName = incoming.providerName;
        copy.id = incoming.id;

        user.authenticationData.add(copy);
    }

    public synchronized void onShutdown() {
        saveUsers();
    }

    private void loadUsers() {
        users.clear();

        if (!dataFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection usersSection = yaml.getConfigurationSection("users");
        if (usersSection == null) return;

        for (String key : usersSection.getKeys(false)) {
            ConfigurationSection userSec = usersSection.getConfigurationSection(key);
            if (userSec == null) continue;

            String playerName = userSec.getString("playerName", key);
            String activeSessionHash = userSec.getString("activeSessionHash", null);
            String activeProvider = userSec.getString("activeProvider", null);

            User user = new User();
            user.playerName = playerName;
            user.activeSessionHash = activeSessionHash;
            user.activeProvider = activeProvider;

            List<?> list = userSec.getList("authenticationData");
            if (list != null) {
                user.authenticationData = new ArrayList<>();
                for (Object o : list) {
                    if (!(o instanceof java.util.Map<?, ?> map)) continue;

                    AuthDataEntry authDataEntry = new AuthDataEntry();
                    Object providerName = map.get("providerName");
                    Object id = map.get("id");

                    authDataEntry.providerName = (providerName instanceof String) ? (String) providerName : null;
                    authDataEntry.id = (id instanceof String) ? (String) id : null;

                    if (authDataEntry.providerName != null && authDataEntry.id != null) {
                        user.authenticationData.add(authDataEntry);
                    }
                }
            }

            users.add(user);
        }
    }

    private void saveUsers() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection usersSection = yaml.createSection("users");

        for (User user : users) {
            if (user.playerName == null) continue;

            ConfigurationSection userSec = usersSection.createSection(user.playerName);
            userSec.set("playerName", user.playerName);
            userSec.set("activeSessionHash", user.activeSessionHash);
            userSec.set("activeProvider", user.activeProvider);

            List<Map<String, Object>> userAuthData = new ArrayList<>();
            if (user.authenticationData != null) {
                for (AuthDataEntry authDataEntry : user.authenticationData) {
                    if (authDataEntry == null) continue;
                    if (authDataEntry.providerName == null || authDataEntry.providerName.isEmpty()) continue;
                    if (authDataEntry.id == null || authDataEntry.id.isEmpty()) continue;

                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("providerName", authDataEntry.providerName);
                    m.put("id", authDataEntry.id);
                    userAuthData.add(m);
                }
            }

            userSec.set("authenticationData", userAuthData);
        }

        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            yaml.save(dataFile);
        }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to save users to " + dataFile.getName() + ": " + e.getMessage());
        }
    }
}