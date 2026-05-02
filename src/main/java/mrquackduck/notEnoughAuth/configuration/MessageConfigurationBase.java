package mrquackduck.notEnoughAuth.configuration;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageConfigurationBase extends ConfigurationBase {
    private final String messagesConfigurationSectionName;

    public MessageConfigurationBase(JavaPlugin plugin, String messagesConfigurationSectionName) {
        super(plugin);
        this.messagesConfigurationSectionName = messagesConfigurationSectionName;
    }

    /**
     * Returns a message from configuration by key without formatting.
     */
    public String getPlainMessage(String key) {
        var message = getString(messagesConfigurationSectionName + '.' + key);
        if (message == null) return String.format("Message %s wasn't found", key);

        if (key.equalsIgnoreCase("prefix")) return message;
        message = message.replace("<prefix>", getPlainMessage("prefix"));

        return message;
    }

    /**
     * Returns a formatted message from configuration by key.
     */
    public TextComponent getMessage(String key) {
        var message = getPlainMessage(key);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}